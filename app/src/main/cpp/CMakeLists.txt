# CMake configuration for Crispify native library
cmake_minimum_required(VERSION 3.22.1)

# Declare the project
project("crispify_llama")

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Add compile options for optimization
add_compile_options(-O3 -Wall -Wextra)

# Enable exceptions for llama.cpp
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fexceptions")

# Find required libraries
find_library(log-lib log)
find_library(android-lib android)

# Create the main JNI library
add_library(crispify_llama SHARED
    crispify_jni.cpp
    llama_wrapper.cpp
    token_callback.cpp
)

# Include directories
target_include_directories(crispify_llama PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${CMAKE_CURRENT_SOURCE_DIR}/llama.cpp/include
    ${CMAKE_CURRENT_SOURCE_DIR}/llama.cpp/common
    ${CMAKE_CURRENT_SOURCE_DIR}/llama.cpp/ggml/include
)

# Link libraries
target_link_libraries(crispify_llama
    ${log-lib}
    ${android-lib}
)

# Add llama.cpp library
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_SERVER OFF CACHE BOOL "" FORCE)
set(LLAMA_NATIVE OFF CACHE BOOL "" FORCE)
set(LLAMA_CUBLAS OFF CACHE BOOL "" FORCE)
set(LLAMA_METAL OFF CACHE BOOL "" FORCE)

add_subdirectory(llama.cpp)

# Build common library
add_library(common STATIC
    llama.cpp/common/common.cpp
    llama.cpp/common/arg.cpp
    llama.cpp/common/log.cpp
    llama.cpp/common/console.cpp
    llama.cpp/common/sampling.cpp
    build-info.cpp
)

target_include_directories(common PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/llama.cpp/include
    ${CMAKE_CURRENT_SOURCE_DIR}/llama.cpp/common
    ${CMAKE_CURRENT_SOURCE_DIR}/llama.cpp/ggml/include
    ${CMAKE_CURRENT_SOURCE_DIR}/llama.cpp/vendor
)

# Link llama and common libraries
target_link_libraries(crispify_llama
    llama
    common
)