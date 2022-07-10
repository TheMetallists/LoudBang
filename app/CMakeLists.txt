# Sets the minimum version of CMake required to build your native library.
# This ensures that a certain set of CMake features is available to
# your build.

cmake_minimum_required(VERSION 3.10.2)

# Specifies a library name, specifies whether the library is STATIC or
# SHARED, and provides relative paths to the source code. You can
# define multiple libraries by adding multiple add_library() commands,
# and CMake builds them for you. When you build your app, Gradle
# automatically packages shared libraries with your APK.

# ./src/main/cppfft/fftw-3.3.8/

project(QuietScream)

# Adds the CMakeLists.txt file located in the specified directory
# as a build dependency.
add_subdirectory( # Specifies the directory of the CMakeLists.txt file.
        ./src/main/cppfft/fftw-3.3.8/
        )

set(wsprd_CSRCS
        src/main/jni/wsprd/wsprd.c
        src/main/jni/wsprd/wsprsim_utils.c
        src/main/jni/wsprd/wsprd_utils.c
        src/main/jni/wsprd/fano.c
        src/main/jni/wsprd/jelinek.c
        src/main/jni/wsprd/tab.c
        src/main/jni/wsprd/nhash.c
        src/main/jni/wsprd/init_random_seed.c
        )

set(wenc_CSRCS
        src/main/jni/lbenc2/wenc.c
        src/main/jni/lbenc2/coder.c
        )

add_library( # Specifies the name of the library.
        QuietScream

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        src/main/jni/libloud.cpp
        src/main/jni/locator_position_interface.cpp
        ${wsprd_CSRCS}
        ${wenc_CSRCS}
        )

target_include_directories(QuietScream PRIVATE ./src/main/cppfft/fftw-3.3.8/api/)
target_link_libraries(QuietScream
        fftw3f
        m
        log
        )
