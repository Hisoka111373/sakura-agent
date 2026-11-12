#include <jni.h>
#include <llama.h>
#include <algorithm>
#include <atomic>
#include <mutex>
#include <stdexcept>
#include <string>
#include <vector>

// Single process engine; Java serializes requests, mutex protects teardown.
namespace {
std::mutex mutex;
std::atomic<bool> cancelled{false};
llama_model *model = nullptr;
llama_context *ctx = nullptr;
llama_sampler *sampler = nullptr;
bool initialized = false;
constexpr int contextSize = 2048;
constexpr int batchSize = 256;
constexpr int outputLimit = 384;

bool abortDecode(void *) { return cancelled.load(); }
bool loadProgress(float, void *) { return !cancelled.load(); }

void unload() {
    if (sampler) llama_sampler_free(sampler);
    if (ctx) llama_free(ctx);
    if (model) llama_model_free(model);
    sampler = nullptr; ctx = nullptr; model = nullptr;
}

void fail(JNIEnv *env, const char *message) {
    env->ThrowNew(env->FindClass("java/lang/IllegalStateException"), message);
}

std::string bytes(JNIEnv *env, jbyteArray value) {
    if (!value) throw std::runtime_error("Missing UTF-8 input.");
    const auto size = env->GetArrayLength(value);
    if (size > 128 * 1024) throw std::runtime_error("Input too large.");
    std::string result(static_cast<size_t>(size), '\0');
    env->GetByteArrayRegion(value, 0, size, reinterpret_cast<jbyte *>(result.data()));
    return result;
}

std::string format(JNIEnv *env, jbyteArray system, jobjectArray roles, jobjectArray texts) {
    const int count = env->GetArrayLength(roles);
    if (count < 1 || count > 12 || count != env->GetArrayLength(texts))
        throw std::runtime_error("Invalid chat history.");
    std::vector<std::string> roleValues{"system"};
    std::vector<std::string> contents{bytes(env, system)};
    roleValues.reserve(count + 1); contents.reserve(count + 1);
    for (int i = 0; i < count; ++i) {
        auto role = static_cast<jstring>(env->GetObjectArrayElement(roles, i));
        const char *raw = env->GetStringUTFChars(role, nullptr);
        if (!raw) throw std::runtime_error("Could not read role.");
        std::string roleText(raw);
        env->ReleaseStringUTFChars(role, raw); env->DeleteLocalRef(role);
        if (roleText != "user" && roleText != "assistant") throw std::runtime_error("Invalid role.");
        auto text = static_cast<jbyteArray>(env->GetObjectArrayElement(texts, i));
        roleValues.push_back(roleText); contents.push_back(bytes(env, text));
        env->DeleteLocalRef(text);
    }
    std::vector<llama_chat_message> messages;
    for (size_t i = 0; i < contents.size(); ++i)
        messages.push_back({roleValues[i].c_str(), contents[i].c_str()});
    const char *templ = llama_model_chat_template(model, nullptr);
    if (!templ) throw std::runtime_error("This GGUF has no chat template. Use a supported instruct model.");
    std::vector<char> buffer(32768);
    int32_t size = llama_chat_apply_template(templ, messages.data(), messages.size(), true, buffer.data(), buffer.size());
    if (size < 0) throw std::runtime_error("Unsupported chat template.");
    if (size >= static_cast<int32_t>(buffer.size())) {
        if (size > 128 * 1024) throw std::runtime_error("Chat too large.");
        buffer.resize(static_cast<size_t>(size) + 1);
        size = llama_chat_apply_template(templ, messages.data(), messages.size(), true, buffer.data(), buffer.size());
        if (size < 0 || size >= static_cast<int32_t>(buffer.size())) throw std::runtime_error("Chat template failed.");
    }
    return std::string(buffer.data(), static_cast<size_t>(size));
}

std::vector<llama_token> tokenize(const std::string &text) {
    const auto vocab = llama_model_get_vocab(model);
    int32_t count = llama_tokenize(vocab, text.data(), text.size(), nullptr, 0, true, true);
    if (count >= 0) throw std::runtime_error("Tokenization failed.");
    std::vector<llama_token> result(static_cast<size_t>(-count));
    count = llama_tokenize(vocab, text.data(), text.size(), result.data(), result.size(), true, true);
    if (count <= 0) throw std::runtime_error("Tokenization failed.");
    result.resize(count);
    return result;
}

bool decode(const std::vector<llama_token> &tokens) {
    auto batch = llama_batch_init(batchSize, 0, 1);
    bool ok = true;
    for (size_t offset = 0; offset < tokens.size(); offset += batchSize) {
        if (cancelled.load()) { ok = false; break; }
        const int count = std::min<size_t>(batchSize, tokens.size() - offset);
        batch.n_tokens = count;
        for (int i = 0; i < count; ++i) {
            const auto index = offset + i;
            batch.token[i] = tokens[index]; batch.pos[i] = index;
            batch.n_seq_id[i] = 1; batch.seq_id[i][0] = 0;
            batch.logits[i] = index == tokens.size() - 1;
        }
        if (llama_decode(ctx, batch) != 0) { ok = false; break; }
    }
    llama_batch_free(batch);
    return ok;
}
} // namespace

extern "C" JNIEXPORT void JNICALL
Java_com_sakura_agent_NativeLlama_nativeLoad(JNIEnv *env, jobject, jstring path) {
    std::lock_guard<std::mutex> guard(mutex);
    cancelled.store(false);
    unload();
    try {
        if (!initialized) { llama_backend_init(); initialized = true; }
        const char *raw = env->GetStringUTFChars(path, nullptr);
        if (!raw) throw std::runtime_error("Could not read model path.");
        std::string filename(raw);
        env->ReleaseStringUTFChars(path, raw);
        auto params = llama_model_default_params();
        params.n_gpu_layers = 0;
        params.progress_callback = loadProgress;
        model = llama_model_load_from_file(filename.c_str(), params);
        if (!model) throw std::runtime_error("GGUF load failed or was cancelled. Check model compatibility and available RAM.");
        auto cp = llama_context_default_params();
        cp.n_ctx = contextSize; cp.n_batch = batchSize; cp.n_ubatch = batchSize;
        cp.n_threads = 4; cp.n_threads_batch = 4;
        cp.abort_callback = abortDecode;
        ctx = llama_init_from_model(model, cp);
        if (!ctx) throw std::runtime_error("Context allocation failed. Try a smaller model.");
        sampler = llama_sampler_chain_init(llama_sampler_chain_default_params());
        llama_sampler_chain_add(sampler, llama_sampler_init_top_k(40));
        llama_sampler_chain_add(sampler, llama_sampler_init_top_p(0.9f, 1));
        llama_sampler_chain_add(sampler, llama_sampler_init_temp(0.7f));
        llama_sampler_chain_add(sampler, llama_sampler_init_dist(LLAMA_DEFAULT_SEED));
    } catch (const std::exception &e) { unload(); fail(env, e.what()); }
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_sakura_agent_NativeLlama_nativeGenerate(JNIEnv *env, jobject, jbyteArray system,
                                                jobjectArray roles, jobjectArray texts) {
    std::lock_guard<std::mutex> guard(mutex);
    try {
        if (!model || !ctx || !sampler) throw std::runtime_error("Load a GGUF model first.");
        auto tokens = tokenize(format(env, system, roles, texts));
        if (tokens.size() + outputLimit >= contextSize)
            throw std::runtime_error("Context limit exceeded (2048 tokens). Start a new chat or shorten the message and memories.");
        llama_memory_clear(llama_get_memory(ctx), true);
        llama_sampler_reset(sampler);
        if (!decode(tokens)) throw std::runtime_error("Prompt processing failed or was cancelled.");
        auto batch = llama_batch_init(1, 0, 1);
        std::string output;
        bool decodeFailed = false;
        for (int step = 0; step < outputLimit && !cancelled.load(); ++step) {
            const auto token = llama_sampler_sample(sampler, ctx, -1);
            const auto vocab = llama_model_get_vocab(model);
            if (llama_vocab_is_eog(vocab, token)) break;
            std::vector<char> piece(256);
            int size = llama_token_to_piece(vocab, token, piece.data(), piece.size(), 0, false);
            if (size < 0) {
                piece.resize(-size);
                size = llama_token_to_piece(vocab, token, piece.data(), piece.size(), 0, false);
            }
            if (size > 0) output.append(piece.data(), size);
            batch.n_tokens = 1; batch.token[0] = token;
            batch.pos[0] = tokens.size() + step;
            batch.n_seq_id[0] = 1; batch.seq_id[0][0] = 0; batch.logits[0] = true;
            if (llama_decode(ctx, batch) != 0) { decodeFailed = true; break; }
        }
        llama_batch_free(batch);
        if (cancelled.load()) throw std::runtime_error("Generation cancelled.");
        if (decodeFailed) throw std::runtime_error("Generation failed. No partial result was saved.");
        if (output.empty()) throw std::runtime_error("The model returned no text.");
        auto result = env->NewByteArray(output.size());
        if (result) env->SetByteArrayRegion(result, 0, output.size(), reinterpret_cast<const jbyte *>(output.data()));
        return result;
    } catch (const std::exception &e) { fail(env, e.what()); return nullptr; }
}

extern "C" JNIEXPORT void JNICALL
Java_com_sakura_agent_NativeLlama_nativeCancel(JNIEnv *, jobject) { cancelled.store(true); }

extern "C" JNIEXPORT void JNICALL
Java_com_sakura_agent_NativeLlama_nativeResetCancellation(JNIEnv *, jobject) { cancelled.store(false); }

extern "C" JNIEXPORT void JNICALL
Java_com_sakura_agent_NativeLlama_nativeUnload(JNIEnv *, jobject) {
    std::lock_guard<std::mutex> guard(mutex); unload();
}
