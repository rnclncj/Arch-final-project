wire clk;
wire [63:2] mem_raddr0;
wire [31:0] mem_rdata0;
wire [63:2] mem_raddr1;
wire [31:0] mem_rdata1;
wire mem_wen;
wire [63:2] mem_waddr;
wire [31:0] mem_wdata;

memory mem(clk,mem_raddr0,mem_rdata0,mem_raddr1,mem_rdata1,mem_wen,mem_waddr,mem_wdata);

assign clk = theClock;
assign mem_raddr1 = F1_stall ? mem_raddr0 + 1 : F1_pc;  
assign mem_raddr0 = M1_is_Ldrb_Pre | M1_isLdrbOff | M1_isLdrPre | M1_isLdrOff | M1_isStrbOff | M1_isStrbPre;
assign mem_waddr = WB_isStrbOff | WB_isStrbPre ? res[63:2] : WB_op1[63:2];
assign mem_wen = WB_valid & (WB_isStrbPre | WB_isStrbPost | WB_isStrbOff);
assign mem_wdata = add[1:0] == 2'b11 ? {WB_vd_vt[7:0], mem_rdata0[23:0]} :
                       add[1:0] == 2'b10 ? {mem_rdata0[31:24], WB_vd_vt[7:0],mem_rdata0[15:0]} :
                       add[1:0] == 2'b01 ? {mem_rdata0[31:16], WB_vd_vt[7:0], mem_rdata0[7:0]} :
                       {mem_rdata0[31:8], WB_vd_vt[7:0]};
