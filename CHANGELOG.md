# 0.1.3
> Published 25 Dec 2018

- Fixed wrong pom dependencies

# 0.1.2
> Published 24 Dec 2018

- Fixed byte channel constructor from an array
- Fixed endGap related errors (#23)
- Introduced suspending consumeEachRemaining (#22)
- Kotlin 1.3.11, kotlinx.coroutines 1.1.0
- Fixed await returned wrong result in sequential implementation (#24)
- `await` and `awaitAtLeast` contract clarified (#24)
- Fixed blocking I/O adapter to use coroutine's event loop

# 0.1.1
> Published 4 Dec 2018

- Fixed ability to implement DefaultPool in common
- Fixed error "Unable to stop reading in state Writing"
- Fixed tryPeek implementation to not consume byte
- Introduced peekCharUtf8
- Added a cpointer constructor to native IoBuffer so that IoBuffer can be used to read AND write on a memory chunk 
- Made ByteChannel pass original cause from the owner job
- Fixed reading UTF-8 lines
- Fixed empty chunk view creation
- Utility functions takeWhile* improvements

# 0.1.0
> Published 15 Nov 2018
Initial release, maven central
