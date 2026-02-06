cmake_minimum_required(VERSION 3.22.1)
project("azura_native")

add_library(native_image SHARED native_image.cpp)

find_library(log-lib log)
target_link_libraries(native_image ${log-lib})