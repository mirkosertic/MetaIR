# ToDo's

This is a short but not complete list of things that need to be done or should be thought about.

- Implement correct handling of memory flow while scheduling nodes
- ClassInit, Put Field / Put Status / WriteArray should not be part of the CFG and should be scheduled
  based on memory and data flow
- Analysis context should be recorded in terms of type access / method invocation, so these
  references can also be resolved during analysis to build a full type hierarchy

