cmake_minimum_required(VERSION 3.31.6)

project(sakura_llm LANGUAGES C CXX)

if (NOT DEFINED LLAMA_SRC OR NOT EXISTS "${LLAMA_SRC}/CMakeLists.txt")
    message(FATAL_ERROR "LLAMA_SRC must point to a llama.cpp checkout")
endif()

set(BUILD_SHARED_LIBS ON CACHE BOOL "Build shared native libraries" FORCE)
set(LLAMA_BUILD_COMMON OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TOOLS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_SERVER OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_APP OFF CACHE BOOL "" FORCE)
set(GGML_NATIVE OFF CACHE BOOL "" FORCE)
set(GGML_OPENMP OFF CACHE BOOL "" FORCE)
set(GGML_LLAMAFILE OFF CACHE BOOL "" FORCE)
set(LLAMA_OPENSSL OFF CACHE BOOL "" FORCE)

add_subdirectory("${LLAMA_SRC}" "${CMAKE_BINARY_DIR}/llama.cpp")

add_library(sakura_llm SHARED sakura_llm.cpp)
target_compile_features(sakura_llm PRIVATE cxx_std_17)
target_link_libraries(sakura_llm PRIVATE llama android log)
