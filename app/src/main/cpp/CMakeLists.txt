# CMake configuration for Crispify native library
cmake_minimum_required(VERSION 3.22.1)

# Declare the project
project("crispify_llama")

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Add compile options for optimization
add_compile_options(-O3 -Wall -Wextra)

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
)

# Link libraries
target_link_libraries(crispify_llama
    ${log-lib}
    ${android-lib}
)

# TODO: Add llama.cpp as subdirectory when integrated
# add_subdirectory(llama.cpp)
# target_link_libraries(crispify_llama llama)