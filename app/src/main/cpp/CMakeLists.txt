cmake_minimum_required(VERSION 3.22.1)
project("azura_native")

add_library(azura_native SHARED native-lib.cpp)

find_library(log-lib log)
target_link_libraries(azura_native ${log-lib})