# ToDo's

This is a short but not complete list of things that need to be done or should be thought about.

- Create PHI for merge nodes on the iDom of that node, as the
  PHI must be visible in all incoming nodes to be set. So the iDom
  of the merge node is the common ancestor of all incoming nodes,
  and this is the best place to create the PHI.
- Compute the CFG Dominator Tree at frame level instead of graph
  level, as we need the dominator tree fully computed while we
  construct the IR graph. The frames are already there, so we can
  use them to compute the dominator tree and store it in the frame.
