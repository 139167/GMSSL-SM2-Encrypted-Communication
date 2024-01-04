# Additional clean files
cmake_minimum_required(VERSION 3.16)

if("${CONFIG}" STREQUAL "" OR "${CONFIG}" STREQUAL "Debug")
  file(REMOVE_RECURSE
  "CMakeFiles/src_autogen.dir/AutogenUsed.txt"
  "CMakeFiles/src_autogen.dir/ParseCache.txt"
  "src_autogen"
  )
endif()
