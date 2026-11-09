#include <jni.h>
#include <android/log.h>
#include <llama.h>

#include <algorithm>
#include <mutex>
#include <string>
#include <vector>

#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "SakuraLLM", __VA_ARGS__)

namespace {
std::mutex g_mutex;
llama_model *g_model = nullptr;
llama_context *g_context = nullptr;
llama_sampler *g_sampler = nullptr;
bool g_backend_initialized = false;
constexpr int kContextTokens = 2048;
constexpr int kBatchTokens = 256;
constexpr int kMaxOutputTokens = 256;

void unloadModel() {
    if (g_sampler) { llama_sampler_free(g_sampler); g_sampler = nullptr; }
    if (g_context) { llama_free(g_context); g_context = nullptr; }
    if (g_model) { llama_model_free(g_model); g_model = nullptr; }
}

std::vector<llama_token> tokenize(const llama_vocab *vocab, const std::string &text) {
    std::vector<llama_token> tokens(std::max<size_t>(text.size() + 16, 128));
    int32_t count = llama_tokenize(vocab, text.c_str(), static_cast<int32_t>(text.size()),
                                   tokens.data(), static_cast<int32_t>(tokens.size()), false, true);
    if (count < 0) {
        tokens.resize(static_cast<size_t>(-count));
        count = llama_tokenize(vocab, text.c_str(), static_cast<int32_t>(text.size()),
                               tokens.data(), static_cast<int32_t>(tokens.size()), false, true);
    }
    if (count < 0) return {};
    tokens.resize(static_cast<size_t>(count));
    return tokens;
}

bool decodePrompt(const std::vector<llama_token> &tokens) {
    llama_batch batch = llama_batch_init(kBatchTokens, 0, 1);
    if (!batch.token) return false;
    bool ok = true;
    for (size_t offset = 0; offset < tokens.size(); offset += kBatchTokens) {
        const int count = static_cast<int>(std::min<size_t>(kBatchTokens, tokens.size() - offset));
        batch.n_tokens = count;
        for (int i = 0; i < count; ++i) {
            const int index = static_cast<int>(offset) + i;
            batch.token[i] = tokens[static_cast<size_t>(index)];
            batch.pos[i] = index;
            batch.n_seq_id[i] = 1;
            batch.seq_id[i][0] = 0;
            batch.logits[i] = index == static_cast<int>(tokens.size()) - 1 ? 1 : 0;
        }
        if (llama_decode(g_context, batch) != 0) { ok = false; break; }
    }
    llama_batch_free(batch);
    return ok;
}

std::string formatChat(const std::string &userPrompt) {
    const char *templateText = llama_model_chat_template(g_model, nullptr);
    if (!templateText) templateText = "";
    std::string systemText = "Eres Sakura, una asistente amable y útil. Responde en español, con claridad y honestidad. No afirmes haber realizado acciones que no ejecutaste.";
    llama_chat_message messages[] = {{"system", systemText.c_str()}, {"user", userPrompt.c_str()}};
    const int32_t capacity = static_cast<int32_t>(std::min<size_t>(systemText.size() + userPrompt.size() + 4096, 32768));
    std::vector<char> buffer(static_cast<size_t>(capacity));
    int32_t size = llama_chat_apply_template(templateText, messages, 2, true, buffer.data(), capacity);
    if (size < 0) return {};
    if (size >= capacity) {
        buffer.resize(static_cast<size_t>(size) + 1);
        size = llama_chat_apply_template(templateText, messages, 2, true, buffer.data(), static_cast<int32_t>(buffer.size()));
        if (size < 0 || static_cast<size_t>(size) >= buffer.size()) return {};
    }
    return std::string(buffer.data(), static_cast<size_t>(size));
}
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_sakura_agent_NativeLlama_nativeLoad(JNIEnv *env, jobject, jstring modelPath) {
    std::lock_guard<std::mutex> lock(g_mutex);
    unloadModel();
    const char *path = env->GetStringUTFChars(modelPath, nullptr);
    if (!path) return env->NewStringUTF("No pude leer la ruta del modelo.");

    if (!g_backend_initialized) {
        llama_backend_init();
        g_backend_initialized = true;
    }

    llama_model_params modelParams = llama_model_default_params();
    modelParams.n_gpu_layers = 0;
    g_model = llama_model_load_from_file(path, modelParams);
    env->ReleaseStringUTFChars(modelPath, path);

    if (!g_model) {
        return env->NewStringUTF("No pude cargar el GGUF. Comprueba el archivo y la memoria disponible.");
    }

    llama_context_params contextParams = llama_context_default_params();
    contextParams.n_ctx = kContextTokens;
    contextParams.n_batch = kBatchTokens;
    contextParams.n_ubatch = kBatchTokens;
    contextParams.n_threads = 4;
    contextParams.n_threads_batch = 4;
    g_context = llama_init_from_model(g_model, contextParams);
    if (!g_context) {
        unloadModel();
        return env->NewStringUTF("No hay memoria suficiente para iniciar el contexto del modelo.");
    }

    auto samplerParams = llama_sampler_chain_default_params();
    g_sampler = llama_sampler_chain_init(samplerParams);
    llama_sampler_chain_add(g_sampler, llama_sampler_init_top_k(40));
    llama_sampler_chain_add(g_sampler, llama_sampler_init_top_p(0.9f, 1));
    llama_sampler_chain_add(g_sampler, llama_sampler_init_temp(0.7f));
    llama_sampler_chain_add(g_sampler, llama_sampler_init_dist(LLAMA_DEFAULT_SEED));
    return env->NewStringUTF("");
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_sakura_agent_NativeLlama_nativeGenerate(JNIEnv *env, jobject, jstring input) {
    std::lock_guard<std::mutex> lock(g_mutex);
    if (!g_model || !g_context || !g_sampler) {
        return env->NewStringUTF("Carga primero un modelo local desde Modelos.");
    }

    const char *raw = env->GetStringUTFChars(input, nullptr);
    if (!raw) return env->NewStringUTF("No pude leer el mensaje.");

    std::string userText(raw);
    env->ReleaseStringUTFChars(input, raw);

    if (userText.size() > 6000) userText.resize(6000);

    const std::string prompt = formatChat(userText);
    if (prompt.empty()) {
        return env->NewStringUTF("Este modelo no proporcionó una plantilla de chat compatible.");
    }

    llama_memory_clear(llama_get_memory(g_context), true);
    llama_sampler_reset(g_sampler);

    auto tokens = tokenize(llama_model_get_vocab(g_model), prompt);
    if (tokens.empty() || tokens.size() >= kContextTokens - kMaxOutputTokens) {
        return env->NewStringUTF("El mensaje excede el contexto local disponible.");
    }

    if (!decodePrompt(tokens)) {
        return env->NewStringUTF("Falló el procesamiento del mensaje en el modelo.");
    }

    llama_batch batch = llama_batch_init(1, 0, 1);
    if (!batch.token) {
        return env->NewStringUTF("No pude reservar memoria para generar la respuesta.");
    }

    std::string output;
    const llama_vocab *vocab = llama_model_get_vocab(g_model);

    for (int step = 0; step < kMaxOutputTokens; ++step) {
        const llama_token token = llama_sampler_sample(g_sampler, g_context, -1);
        llama_sampler_accept(g_sampler, token);

        if (llama_vocab_is_eog(vocab, token)) break;

        char piece[512];
        const int32_t pieceSize = llama_token_to_piece(vocab, token, piece, sizeof(piece), 0, false);
        if (pieceSize > 0) {
            output.append(piece, static_cast<size_t>(pieceSize));
        }

        batch.n_tokens = 1;
        batch.token[0] = token;
        batch.pos[0] = static_cast<llama_pos>(tokens.size()) + step;
        batch.n_seq_id[0] = 1;
        batch.seq_id[0][0] = 0;
        batch.logits[0] = 1;

        if (llama_decode(g_context, batch) != 0) break;
    }

    llama_batch_free(batch);
    return env->NewStringUTF(output.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_sakura_agent_NativeLlama_nativeUnload(JNIEnv *, jobject) {
    std::lock_guard<std::mutex> lock(g_mutex);
    unloadModel();
}
