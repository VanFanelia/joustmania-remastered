#!/bin/bash


echo "downloading PS move API"
#install psmoveapi (currently adangert's for opencv 3 support)
rm -rf psmoveapi
git clone --recursive https://github.com/thp/psmoveapi.git || exit -1
cd psmoveapi || exit -1
git checkout 8a1f8d035e9c82c5c134d848d9fbb4dd37a34b58 || exit -1

echo "compiling PS move API components"
mkdir build
cd build
cmake .. \
  -DPSMOVE_BUILD_CSHARP_BINDINGS:BOOL=OFF \
  -DPSMOVE_BUILD_EXAMPLES:BOOL=OFF \
  -DPSMOVE_BUILD_JAVA_BINDINGS:BOOL=ON \
  -DPSMOVE_BUILD_OPENGL_EXAMPLES:BOOL=OFF \
  -DPSMOVE_BUILD_PROCESSING_BINDINGS:BOOL=OFF \
  -DPSMOVE_BUILD_TESTS:BOOL=OFF \
  -DPSMOVE_BUILD_TRACKER:BOOL=OFF \
  -DPSMOVE_USE_PSEYE:BOOL=OFF || exit -1

make -j3 || exit -1

