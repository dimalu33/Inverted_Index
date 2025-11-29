cmake_minimum_required(VERSION 3.28)
project(Cppclient)

set(CMAKE_CXX_STANDARD 20)

add_executable(Cppclient main.cpp)

if(WIN32)
    target_link_libraries(Cppclient ws2_32)
endif()