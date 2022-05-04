`timescale 1ps/1ps

module main();


    // There shall be a clock
    wire clk;

    // ... and a counter that tracks cycles
    wire halt;               // set this wire to 1 to halt the processor
    wire wb_has_instruction; // set this wire to 1 if WB has an instruction
    counter cnt(clk,wb_has_instruction,halt);

    // ... and memory
    wire [63:2] mem_raddr0;
    wire [31:0] mem_rdata0;
    wire [63:2] mem_raddr1;
    wire [31:0] mem_rdata1;
    wire mem_wen;
    wire [63:2] mem_waddr;
    wire [31:0] mem_wdata;

    memory mem(clk,mem_raddr0,mem_rdata0,mem_raddr1,mem_rdata1,mem_wen,mem_waddr,mem_wdata);

    // ... and registers
    wire [4:0] reg_raddr0;
    wire [63:0] reg_rdata0;
    wire [4:0] reg_raddr1;
    wire [63:0] reg_rdata1;
    wire reg_wen;
    wire [4:0] reg_waddr;
    wire [63:0] reg_wdata;
    registers regs(clk,reg_raddr0,reg_rdata0,reg_raddr1,reg_rdata1,reg_wen,reg_waddr,reg_wdata);

    reg D_was_stalling = 0;
