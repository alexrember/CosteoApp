cmake_minimum_required(VERSION 3.22.1)
project("costeoapp")

add_library(costeoapp-secrets SHARED secrets.cpp)
target_link_libraries(costeoapp-secrets log)
