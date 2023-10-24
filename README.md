# WASM PROCESSOR
This is a processor design for WebAssembly ISA (work in progress).

It is my hobby project and can serve as an exploration of secure ISA design for microprocessors.

Project progress:
- [x] Overall datapath for instruction & data fetching and execution
- [x] i32 arithmetic & logic instructions
- [x] `block` and `loop` structures and conditional branching
- [ ] Function calls
- [ ] Handle I/O and other side effects in a WebAssembly style (imported function as system call)
- [ ] An extra pipeline behind standard WebAssembly toolchain to convert `.wasm` binaries into runtime ROMs (function tables, instruction ROM, etc.)
