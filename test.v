`timescale 1ps/1ps

module main();


    // There shall be a clock
    wire clk;

    // ... and a counter that tracks cycles
    wire halt;               // set this wire to 1 to halt the processor
    wire wb_has_instruction; // set this wire to 1 if WB has an instruction
    // counter cnt(clk,wb_has_instruction,halt);

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

    /////////////////
    // Fetch 1     //
    /////////////////

    reg [63:2]F1_pc = 62'h0000000000000000;
    reg F1_valid = 1;
    wire F1_stall;
    wire F1_modified = F1_valid & WB_M_add_valid & F1_pc == WB_M_add;
    always @(posedge clk) begin
        F1_pc <= jmp ? ((WB_imm[63:2]) + WB_pc) : 
                 WB_flush ? WB_pc + 1:
                 D_stall | X_stall | WB_stall ? F1_pc:
                 WB_was_stalling ? WB_pc+1 :
                 ~X_stall & X_was_stalling ? X_pc+1: 
                 ~D_stall & D_was_stalling & D_valid ? D_pc+1 :
                 F1_stall ? F1_pc : F1_pc+1;
    end

    /////////////
    // Fetch 2 //
    /////////////
    
    reg [63:2]F2_pc;
    reg F2_valid = 0;
    wire F2_modified = F2_valid & WB_M_add_valid & F2_pc == WB_M_add;

    always @(posedge clk) begin
        F2_valid <= ~F1_stall & ~D_stall & F1_valid & ~WB_flush & ~jmp & ~WB_was_stalling & ~X_was_stalling & ~(D_was_stalling & D_valid);
        if (~D_stall) begin
	        F2_pc <= F1_pc;
        end
    end

    // W1
    reg [63:2]W1_pc;
    reg W1_valid = 0;
    wire W1_modified = W1_valid & WB_M_add_valid & W1_pc == WB_M_add;

    always @(posedge clk) begin
        W1_valid <= ~D_stall & F2_valid & ~WB_flush;
        if (~D_stall) begin
	        W1_pc <= F2_pc;
        end
    end
   
    ////////////
    // Decode //
    ////////////

    reg [63:2]D_pc;
    reg [31:0] D_ins;
    reg D_valid = 0;
    wire D_need_to_stall = R1_R1_num_valid & R1_R1_num == D_rn | R1_R1_num_valid & R1_R1_num == D_rd_rt | 
                           R1_R2_num_valid & R1_R2_num == D_rn | R1_R2_num_valid & R1_R2_num == D_rd_rt |
                           R2_R1_num_valid & R2_R1_num == D_rn | R2_R1_num_valid & R2_R1_num == D_rd_rt | 
                           R2_R2_num_valid & R2_R2_num == D_rn | R2_R2_num_valid & R2_R2_num == D_rd_rt |
                           W2_R2_num_valid &  (W2_R2_num == D_rn | W2_R2_num == D_rd_rt) |
                           W2_R1_num_valid & (W2_R1_num == D_rn | W2_R1_num == D_rd_rt) |
                           X_R1_num_valid & X_R1_num == D_rn | X_R1_num_valid & X_R1_num == D_rd_rt | 
                           X_R2_num_valid & X_R2_num == D_rn | X_R2_num_valid & X_R2_num == D_rd_rt |
                           M1_R1_num_valid & M1_R1_num == D_rn | M1_R1_num_valid & M1_R1_num == D_rd_rt | 
                           M1_R2_num_valid & M1_R2_num == D_rn | M1_R2_num_valid & M1_R2_num == D_rd_rt |
                           M2_R1_num_valid & M2_R1_num == D_rn | M2_R1_num_valid & M2_R1_num == D_rd_rt |
                           M2_R2_num_valid & M2_R2_num == D_rn | M2_R2_num_valid & M2_R2_num == D_rd_rt |
                           WB_R1_num_valid & WB_R1_num == D_rn | WB_R1_num_valid & WB_R1_num == D_rd_rt | 
                           WB_R2_num_valid & WB_R2_num == D_rn | WB_R2_num_valid & WB_R2_num == D_rd_rt;
    wire D_stall = D_need_to_stall & D_valid | X_stall;
    reg D_was_stalling = 0;
    wire D_modified = D_valid & WB_M_add_valid & D_pc == WB_M_add;

    wire D_isAdrp = D_ins[31] == 1 & D_ins[28:24] == 5'b10000;
    wire D_isAddi = D_ins[30:23] == 8'b00100010;
    wire D_isMovz = D_ins[30:23] == 8'b10100101;
    wire D_isLdrPre = D_ins[31] == 1 & D_ins[29:21] == 9'b111000010 & D_ins[11:10] == 2'b11;
    wire D_isLdrPost = D_ins[31] == 1 & D_ins[29:21] == 9'b111000010 & D_ins[11:10] == 2'b01;
    wire D_isLdrOff = D_ins[31] == 1 & D_ins[29:22] == 8'b11100101;
    wire D_isStrbPre = D_ins[31:21] == 11'b00111000000 & D_ins[11:10] == 2'b11;
    wire D_isStrbPost = D_ins[31:21] == 11'b00111000000 & D_ins[11:10] == 2'b01;
    wire D_isStrbOff = D_ins[31:22] == 10'b0011100100;
    wire D_isLdrbPre = D_ins[31:21] == 11'b00111000010 & D_ins[11:10] == 2'b11;
    wire D_isLdrbPost = D_ins[31:21] == 11'b00111000010 & D_ins[11:10] == 2'b01;
    wire D_isLdrbOff = D_ins[31:22] == 10'b0011100101;
    wire D_isCbnz = D_ins[30:24] == 7'b0110101;
    wire[63:0] D_imm = D_isAddi ? (D_sh ? {D_imm12[21:10], 12'b0}: D_imm12[21:10]) :
                       D_isAdrp ? {{31{D_immhi[23]}}, D_immhi, D_immlo, 12'b0} :
                       D_isCbnz ? {{43{D_imm19[23]}}, D_imm19[23:5], 2'b0} :
                       D_isMovz ? (D_hw == 2'b00 ? D_imm16 : (D_hw == 2'b01 ? {D_imm16, 16'b0} : 
                           (D_hw == 2'b10 ? {D_imm16, 32'b0} : {D_imm16, 48'b0}))) :
                       D_isLdrbPre | D_isLdrbPost | D_isLdrPre | D_isLdrPost | D_isStrbPre |
                        D_isStrbPost ? {{55{D_imm9[20]}}, D_imm9[20:12]} : 
                       D_isLdrbOff | D_isLdrOff | D_isStrbOff ? 
                       (D_size[31:30] == 2'b11 ? {D_imm12[21:10], 3'b0} : D_size[31:30] == 2'b10 ? {D_imm12[21:10], 2'b0} : D_imm12[21:10]) : 0;

    wire [9:5]D_rn = D_ins[9:5];
    wire [4:0]D_rd_rt = D_ins[4:0];
    wire D_sf = D_ins[31];
    wire D_sh = D_ins[22];
    wire [21:10]D_imm12 = D_ins[21:10];
    wire [30:29]D_immlo = D_ins[30:29];
    wire [23:5]D_immhi = D_ins[23:5];
    wire [23:5]D_imm19 = D_ins[23:5];
    wire [20:12]D_imm9 = D_ins[20:12];
    wire [20:5]D_imm16 = D_ins[20:5];
    wire [22:21]D_hw = D_ins[22:21];
    wire [31:30]D_size = D_ins[31:30];

    always @(posedge clk) begin
        D_valid <= ~X_stall & (W1_valid | D_stall) & ~WB_flush;
        if (~X_stall) begin
            D_was_stalling <= D_valid & D_stall & (~X_stall & (W1_valid | D_stall) & ~WB_flush);
            D_pc <= D_stall ? D_pc : W1_pc;
            D_ins <= D_stall ? D_ins : mem_rdata1;
        end
    end

    // R1 //
    reg [63:2]R1_pc;
    reg R1_valid = 0;
    wire R1_modified = R1_valid & WB_M_add_valid & R1_pc == WB_M_add;

    reg R1_isAdrp;
    reg R1_isAddi;
    reg R1_isMovz;
    reg R1_isLdrPre;
    reg R1_isLdrPost;
    reg R1_isLdrOff;
    reg R1_isStrbPre;
    reg R1_isStrbPost;
    reg R1_isStrbOff;
    reg R1_isLdrbPre;
    reg R1_isLdrbPost;
    reg R1_isLdrbOff;
    reg R1_isCbnz;
    reg [9:5]R1_rn;
    reg [4:0]R1_rd_rt;
    reg R1_sf;
    reg R1_sh;
    reg [63:0] R1_imm;
    reg [22:21]R1_hw;
    reg [31:30]R1_size;
    wire R1_R1_num_valid = R1_valid & (R1_isLdrPre | R1_isLdrPost | R1_isLdrbPre | R1_isLdrbPost |
                          R1_isStrbPre | R1_isStrbPost);
    wire R1_R2_num_valid = R1_valid & (R1_isAddi | R1_isAdrp | R1_isMovz | R1_isLdrbOff | R1_isLdrbPost |
                          R1_isLdrbPre | R1_isLdrOff | R1_isLdrPre | R1_isLdrPost);
    wire [4:0]R1_R1_num = R1_rn;
    wire [4:0]R1_R2_num = R1_rd_rt;

    assign reg_raddr0 = R1_rn;
    assign reg_raddr1 = R1_rd_rt;

    always @(posedge clk) begin
        R1_valid <= ~X_stall & ~D_stall & D_valid & ~WB_flush;
        if (~X_stall) begin
            R1_pc <= D_pc;

            R1_isAdrp <= D_isAdrp;
            R1_isAddi <= D_isAddi;
            R1_isMovz <= D_isMovz;
            R1_isLdrPre <= D_isLdrPre;
            R1_isLdrPost <= D_isLdrPost;
            R1_isLdrOff <= D_isLdrOff;
            R1_isStrbPre <= D_isStrbPre;
            R1_isStrbPost <= D_isStrbPost;
            R1_isStrbOff <= D_isStrbOff;
            R1_isLdrbPre <= D_isLdrbPre;
            R1_isLdrbPost <= D_isLdrbPost;
            R1_isLdrbOff <= D_isLdrbOff;
            R1_isCbnz <= D_isCbnz;
            R1_rn <= D_rn;
            R1_rd_rt <= D_rd_rt;
            R1_sf <= D_sf;
            R1_sh <= D_sh;
            R1_imm <= D_imm;
            R1_hw <= D_hw;
            R1_size <= D_size;
        end
    end
    
    // R2 //
    reg [63:2]R2_pc;
    reg R2_valid = 0;
    wire R2_modified = R2_valid & WB_M_add_valid & R2_pc == WB_M_add;

    reg R2_isAdrp;
    reg R2_isAddi;
    reg R2_isMovz;
    reg R2_isLdrPre;
    reg R2_isLdrPost;
    reg R2_isLdrOff;
    reg R2_isStrbPre;
    reg R2_isStrbPost;
    reg R2_isStrbOff;
    reg R2_isLdrbPre;
    reg R2_isLdrbPost;
    reg R2_isLdrbOff;
    reg R2_isCbnz;
    reg [9:5]R2_rn;
    reg [4:0]R2_rd_rt;
    reg R2_sf;
    reg R2_sh;
    reg [63:0] R2_imm;
    reg [22:21]R2_hw;
    reg [31:30]R2_size;
    reg R2_R1_num_valid;
    reg R2_R2_num_valid;
    reg [4:0]R2_R1_num;
    reg [4:0]R2_R2_num;

    always @(posedge clk) begin
        R2_valid <= ~X_stall & R1_valid & ~WB_flush;
        if (~X_stall) begin
            R2_pc <= R1_pc;

            R2_isAdrp <= R1_isAdrp;
            R2_isAddi <= R1_isAddi;
            R2_isMovz <= R1_isMovz;
            R2_isLdrPre <= R1_isLdrPre;
            R2_isLdrPost <= R1_isLdrPost;
            R2_isLdrOff <= R1_isLdrOff;
            R2_isStrbPre <= R1_isStrbPre;
            R2_isStrbPost <= R1_isStrbPost;
            R2_isStrbOff <= R1_isStrbOff;
            R2_isLdrbPre <= R1_isLdrbPre;
            R2_isLdrbPost <= R1_isLdrbPost;
            R2_isLdrbOff <= R1_isLdrbOff;
            R2_isCbnz <= R1_isCbnz;
            R2_rn <= R1_rn;
            R2_rd_rt <= R1_rd_rt;
            R2_sf <= R1_sf;
            R2_sh <= R1_sh;
            R2_imm <= R1_imm;
            R2_hw <= R1_hw;
            R2_size <= R1_size;
            R2_R1_num_valid <= R1_R1_num_valid & R1_valid;
            R2_R2_num_valid <= R1_R2_num_valid & R1_valid;
            R2_R1_num <= R1_R1_num;
            R2_R2_num <= R1_R2_num;
        end
    end  

    // W2
    reg [63:2]W2_pc;
    reg W2_valid = 0;
    wire W2_modified = W2_valid & WB_M_add_valid & W2_pc == WB_M_add;

    reg W2_isAdrp;
    reg W2_isAddi;
    reg W2_isMovz;
    reg W2_isLdrPre;
    reg W2_isLdrPost;
    reg W2_isLdrOff;
    reg W2_isStrbPre;
    reg W2_isStrbPost;
    reg W2_isStrbOff;
    reg W2_isLdrbPre;
    reg W2_isLdrbPost;
    reg W2_isLdrbOff;
    reg W2_isCbnz;
    reg [9:5]W2_rn;
    reg [4:0]W2_rd_rt;
    reg W2_sf;
    reg W2_sh;
    reg [63:0] W2_imm;
    reg [22:21]W2_hw;
    reg [31:30]W2_size;
    reg W2_R1_num_valid;
    reg W2_R2_num_valid;
    reg [4:0]W2_R1_num;
    reg [4:0]W2_R2_num;

    always @(posedge clk) begin
        W2_valid <= ~X_stall & R2_valid & ~WB_flush;
        if (~X_stall) begin
            W2_pc <= R2_pc;

            W2_isAdrp <= R2_isAdrp;
            W2_isAddi <= R2_isAddi;
            W2_isMovz <= R2_isMovz;
            W2_isLdrPre <= R2_isLdrPre;
            W2_isLdrPost <= R2_isLdrPost;
            W2_isLdrOff <= R2_isLdrOff;
            W2_isStrbPre <= R2_isStrbPre;
            W2_isStrbPost <= R2_isStrbPost;
            W2_isStrbOff <= R2_isStrbOff;
            W2_isLdrbPre <= R2_isLdrbPre;
            W2_isLdrbPost <= R2_isLdrbPost;
            W2_isLdrbOff <= R2_isLdrbOff;
            W2_isCbnz <= R2_isCbnz;
            W2_rn <= R2_rn;
            W2_rd_rt <= R2_rd_rt;
            W2_sf <= R2_sf;
            W2_sh <= R2_sh;
            W2_imm <= R2_imm;
            W2_hw <= R2_hw;
            W2_size <= R2_size;
            W2_R1_num_valid <= R2_R1_num_valid & R2_valid;
            W2_R2_num_valid <= R2_R2_num_valid & R2_valid;
            W2_R1_num <= R2_R1_num;
            W2_R2_num <= R2_R2_num;
        end
    end  
  
    // X //
    wire X_modified = X_valid & WB_M_add_valid & X_pc == WB_M_add;
    wire is_xzr = (X_isCbnz | X_isAdrp | X_isMovz | ldr_str_offset | ldr_str_shift) & X_rd_rt == 31;
    reg [63:0]X_vn;
    reg [63:0]X_vd_vt;
    wire ldr_str_shift =  X_isLdrbPre | X_isLdrbPost | X_isLdrPre | X_isLdrPost | X_isStrbPre | X_isStrbPost;
    wire ldr_str_offset = X_isLdrbOff | X_isLdrOff | X_isStrbOff;
    wire [63:0]X_op1 = (X_isAddi | ldr_str_shift | ldr_str_offset) ? X_vn :
               X_isCbnz ? X_vd_vt :
               X_isAdrp ? {X_pc, 2'b0} : 0;
    reg [63:2]X_pc;
    reg X_valid = 0;
    wire X_need_to_stall = (ldr_str_offset | ldr_str_shift) & M1_M_add_valid & M1_M_add == X_M_add |
                           M2_M_add_valid & M2_M_add == X_M_add | WB_M_add_valid & WB_M_add == X_M_add;
    wire X_stall = X_need_to_stall & X_valid | WB_stall;
    reg X_was_stalling = 0;
    wire[63:0] X_add = X_op1 + X_imm;
    wire[63:2] X_M_add = X_isStrbPost | X_isLdrbPost | X_isLdrPost ? X_op1[63:2] : X_add[63:2];

    reg X_isAdrp;
    reg X_isAddi;
    reg X_isMovz;
    reg X_isLdrPre;
    reg X_isLdrPost;
    reg X_isLdrOff;
    reg X_isStrbPre;
    reg X_isStrbPost;
    reg X_isStrbOff;
    reg X_isLdrbPre;
    reg X_isLdrbPost;
    reg X_isLdrbOff;
    reg X_isCbnz;
    reg [9:5]X_rn;
    reg [4:0]X_rd_rt;
    reg X_sf;
    reg X_sh;
    reg [63:0]X_imm;
    reg [22:21]X_hw;
    reg [31:30]X_size;
    reg X_R1_num_valid;
    reg X_R2_num_valid;
    reg [4:0]X_R1_num;
    reg [4:0]X_R2_num;
    

    always @(posedge clk) begin

        X_valid <= ~WB_stall & (W2_valid | X_stall) & ~WB_flush;
        X_pc <= X_stall ? X_pc: W2_pc;
        X_was_stalling <= X_stall & X_valid & (~WB_stall & (W2_valid | X_stall) & ~WB_flush);
        X_vn <= X_stall ? X_vn : reg_rdata0;
        X_vd_vt <= X_stall ? X_vd_vt : reg_rdata1;
        
        if (~X_stall) begin
            X_isAdrp <= W2_isAdrp;
            X_isAddi <= W2_isAddi;
            X_isMovz <= W2_isMovz;
            X_isLdrPre <= W2_isLdrPre;
            X_isLdrPost <= W2_isLdrPost;
            X_isLdrOff <= W2_isLdrOff;
            X_isStrbPre <= W2_isStrbPre;
            X_isStrbPost <= W2_isStrbPost;
            X_isStrbOff <= W2_isStrbOff;
            X_isLdrbPre <= W2_isLdrbPre;
            X_isLdrbPost <= W2_isLdrbPost;
            X_isLdrbOff <= W2_isLdrbOff;
            X_isCbnz <= W2_isCbnz;
            X_rn <= W2_rn;
            X_rd_rt <= W2_rd_rt;
            X_sf <= W2_sf;
            X_sh <= W2_sh;
            X_imm <= W2_imm;
            X_hw <= W2_hw;
            X_size <= W2_size;
            X_R1_num_valid <= W2_R1_num_valid & W2_valid;
            X_R2_num_valid <= W2_R2_num_valid & W2_valid;
            X_R1_num <= W2_R1_num;
            X_R2_num <= W2_R2_num;
        end
    end       

    // M1 //
    assign mem_raddr0 = M1_isLdrbPre | M1_isLdrbOff | M1_isLdrPre | M1_isLdrOff | M1_isStrbOff | M1_isStrbPre ?
                        (M1_op1 + M1_imm)/4 : M1_op1[63:2];
    assign mem_raddr1 = F1_stall ? mem_raddr0 + 1 : F1_pc;
    wire M1_modified = M1_valid & WB_M_add_valid & M1_pc == WB_M_add;

    reg [63:2]M1_pc;
    reg M1_valid = 0;
    assign F1_stall = F1_valid & M1_valid & (M1_isLdrOff | M1_isLdrPre | M1_isLdrPost) & M1_size == 2'b11;
    reg M1_isAdrp;
    reg M1_isAddi;
    reg M1_isMovz;
    reg M1_isLdrPre;
    reg M1_isLdrPost;
    reg M1_isLdrOff;
    reg M1_isStrbPre;
    reg M1_isStrbPost;
    reg M1_isStrbOff;
    reg M1_isLdrbPre;
    reg M1_isLdrbPost;
    reg M1_isLdrbOff;
    reg M1_isCbnz;
    reg [63:0]M1_vd_vt;
    reg [9:5]M1_rn;
    reg [4:0]M1_rd_rt;
    reg M1_sf;
    reg M1_sh;
    reg [22:21]M1_hw;
    reg [31:30]M1_size;
    reg [63:0]M1_imm;
    reg [63:0]M1_op1;
    reg M1_R1_num_valid;
    reg M1_R2_num_valid;
    reg [4:0]M1_R1_num;
    reg [4:0]M1_R2_num;
    wire M1_M_add_valid = M1_valid & (M1_isStrbOff | M1_isStrbPre | M1_isStrbPost) 
    & ~(M1_M_add === 62'bxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx);
    wire [63:0]M1_add = M1_op1 + M1_imm;
    wire [63:2]M1_M_add = M1_isStrbPost ? M1_op1[63:2] : M1_add[63:2];

    always @(posedge clk) begin
 
            M1_valid <= ~X_stall & X_valid & ~WB_flush;
            M1_pc <= X_pc;

            M1_isAdrp <= X_isAdrp;
            M1_isAddi <= X_isAddi;
            M1_isMovz <= X_isMovz;
            M1_isLdrPre <= X_isLdrPre;
            M1_isLdrPost <= X_isLdrPost;
            M1_isLdrOff <= X_isLdrOff;
            M1_isStrbPre <= X_isStrbPre;
            M1_isStrbPost <= X_isStrbPost;
            M1_isStrbOff <= X_isStrbOff;
            M1_isLdrbPre <= X_isLdrbPre;
            M1_isLdrbPost <= X_isLdrbPost;
            M1_isLdrbOff <= X_isLdrbOff;
            M1_isCbnz <= X_isCbnz;
            M1_vd_vt <= is_xzr ? 64'b0 : X_vd_vt;
            M1_rn <= X_rn;
            M1_rd_rt <= X_rd_rt;
            M1_sf <= X_sf;
            M1_sh <= X_sh;
            M1_hw <= X_hw;
            M1_size <= X_size;
            M1_imm <= X_imm;
            M1_op1 <= X_op1;
            M1_R1_num_valid <= X_R1_num_valid & X_valid;
            M1_R2_num_valid <= X_R2_num_valid & X_valid;
            M1_R1_num <= X_R1_num;
            M1_R2_num <= X_R2_num;
    end  

    // M2 // 
    reg [63:2]M2_pc;
    reg M2_valid = 0;
    wire M2_modified = M2_valid & WB_M_add_valid & M2_pc == WB_M_add;

    reg M2_isAdrp;
    reg M2_isAddi;
    reg M2_isMovz;
    reg M2_isLdrPre;
    reg M2_isLdrPost;
    reg M2_isLdrOff;
    reg M2_isStrbPre;
    reg M2_isStrbPost;
    reg M2_isStrbOff;
    reg M2_isLdrbPre;
    reg M2_isLdrbPost;
    reg M2_isLdrbOff;
    reg M2_isCbnz;
    reg [63:0]M2_vd_vt;
    reg [9:5]M2_rn;
    reg [4:0]M2_rd_rt;
    reg M2_sf;
    reg M2_sh;
    reg [22:21]M2_hw;
    reg [31:30]M2_size;
    reg [63:0]M2_imm;
    reg [63:0]M2_op1;
    reg M2_R1_num_valid;
    reg M2_R2_num_valid;
    reg [4:0]M2_R1_num;
    reg [4:0]M2_R2_num;
    reg M2_M_add_valid;
    reg [63:2]M2_M_add;

    always @(posedge clk) begin

            M2_valid <= M1_valid & ~WB_flush & ~WB_stall; // & ~(WB_pc == M2_pc & WB_valid & M2_valid)
            M2_pc <= M1_pc;

            M2_isAdrp <= M1_isAdrp;
            M2_isAddi <= M1_isAddi;
            M2_isMovz <= M1_isMovz;
            M2_isLdrPre <= M1_isLdrPre;
            M2_isLdrPost <= M1_isLdrPost;
            M2_isLdrOff <= M1_isLdrOff;
            M2_isStrbPre <= M1_isStrbPre;
            M2_isStrbPost <= M1_isStrbPost;
            M2_isStrbOff <= M1_isStrbOff;
            M2_isLdrbPre <= M1_isLdrbPre;
            M2_isLdrbPost <= M1_isLdrbPost;
            M2_isLdrbOff <= M1_isLdrbOff;
            M2_isCbnz <= M1_isCbnz;
            M2_vd_vt <= M1_vd_vt;
            M2_rn <= M1_rn;
            M2_rd_rt <= M1_rd_rt;
            M2_sf <= M1_sf;
            M2_sh <= M1_sh;
            M2_hw <= M1_hw;
            M2_size <= M1_size;
            M2_imm <= M1_imm;
            M2_op1 <= M1_op1;
            M2_R1_num_valid <= M1_R1_num_valid & M1_valid;
            M2_R2_num_valid <= M1_R2_num_valid & M1_valid;
            M2_R1_num <= M1_R1_num;
            M2_R2_num <= M1_R2_num;
            M2_M_add_valid <= M1_M_add_valid & M1_valid;
            M2_M_add <= M1_M_add;
    end

    ////////
    // WB //
    ////////mem
    wire [63:0]mdata = {mem_rdata1, mem_rdata0};
    reg [63:0]prev_mdata;
    reg WB_valid = 0;
    reg [63:2] WB_pc;
    assign wb_has_instruction = WB_valid & ~WB_was_stalling;
    wire [63:0]res = WB_op1 + WB_imm;
    assign mem_wen = WB_valid & (WB_isStrbPre | WB_isStrbPost | WB_isStrbOff);
    assign reg_wen = WB_valid & (~WB_was_stalling & (WB_isAddi | WB_isAdrp & WB_rd_rt != 31 |
                     WB_isMovz & WB_rd_rt != 31 | WB_isLdrPre | WB_isLdrPost | WB_isLdrbPre |
                     WB_isLdrbPost | WB_isStrbPre | WB_isStrbPost) |
                     WB_was_stalling & (WB_rd_rt != 31) & (WB_isLdrPre | WB_isLdrPost |
                     WB_isLdrOff | WB_isLdrbPre | WB_isLdrbPost | WB_isLdrbOff));
    assign mem_waddr = WB_isStrbOff | WB_isStrbPre ? res[63:2] : WB_op1[63:2];
    assign mem_wdata = add[1:0] == 2'b11 ? {WB_vd_vt[7:0], mem_rdata0[23:0]} :
                       add[1:0] == 2'b10 ? {mem_rdata0[31:24], WB_vd_vt[7:0],mem_rdata0[15:0]} :
                       add[1:0] == 2'b01 ? {mem_rdata0[31:16], WB_vd_vt[7:0], mem_rdata0[7:0]} :
                       {mem_rdata0[31:8], WB_vd_vt[7:0]};
    assign reg_waddr = WB_isAddi | WB_isAdrp | WB_isMovz | WB_was_stalling ? WB_rd_rt : WB_rn;
    assign reg_wdata = WB_isAddi ? (WB_sf ? res : res[31:0]) : 
                       WB_isAdrp ? WB_imm :
                       WB_isMovz ? WB_imm :
                       (~WB_was_stalling & (WB_isLdrPre | WB_isLdrPost | WB_isLdrbPre | WB_isLdrbPost |
                            WB_isStrbPre | WB_isStrbPost)) ? res :
                       WB_was_stalling ? (WB_isLdrPre | WB_isLdrOff | WB_isLdrPost ? (WB_size[30] ? prev_mdata : prev_mdata[31:0]) :
                       (add[1:0] == 2'b00 ? prev_mdata[7:0] : add[1:0] == 2'b01 ? prev_mdata[15:8] : 
                       add[1:0] == 2'b10 ? prev_mdata[23:16] : prev_mdata[31:24])): 0;
    wire [63:0]add = WB_op1 + (WB_isLdrbPre | WB_isLdrbOff | WB_isStrbPre | WB_isStrbOff | WB_isLdrPre | WB_isLdrPost ? WB_imm : 0);
    wire jmp = WB_valid & WB_isCbnz & (WB_sf ? WB_vd_vt != 0 : WB_vd_vt[31:0] != 0);
    wire WB_flush = WB_valid & (((~(jmp === 1'bx)) & jmp) | M2_modified | M1_modified | X_modified | R2_modified | R1_modified | D_modified |
                    F2_modified | F1_modified & ~(F1_pc === 62'hxxxxxxxxxxxxxxxx) | W2_modified | W1_modified);
    wire WB_stall = WB_valid & (WB_isLdrbOff | WB_isLdrbPre | WB_isLdrbPost 
                              | WB_isLdrOff | WB_isLdrPre | WB_isLdrPost) & ~WB_was_stalling & ~WB_flush;
    reg WB_was_stalling = 0;

    reg WB_isAdrp;
    reg WB_isAddi;
    reg WB_isMovz;
    reg WB_isLdrPre;
    reg WB_isLdrPost;
    reg WB_isLdrOff;
    reg WB_isStrbPre;
    reg WB_isStrbPost;
    reg WB_isStrbOff;
    reg WB_isLdrbPre;
    reg WB_isLdrbPost;
    reg WB_isLdrbOff;
    reg WB_isCbnz;
    reg [63:0]WB_vd_vt;
    reg [9:5]WB_rn;
    reg [4:0]WB_rd_rt;
    reg WB_sf;
    reg WB_sh;
    reg [22:21]WB_hw;
    reg [31:30]WB_size;
    reg [63:0]WB_imm;
    reg [63:0]WB_op1;
    reg WB_R1_num_valid;
    reg WB_R2_num_valid;
    reg [4:0]WB_R1_num;
    reg [4:0]WB_R2_num;
    reg WB_M_add_valid;
    reg [63:2]WB_M_add;
    wire print = mem_wen & ((WB_isStrbOff | WB_isStrbPre) & res == 64'hFFFFFFFFFFFFFFFF | 
            WB_isStrbPost & WB_op1 == 64'hFFFFFFFFFFFFFFFF);

    assign halt = WB_valid & ~(WB_isAdrp | WB_isAddi | WB_isMovz | WB_isLdrbPre |
                  WB_isLdrbPost | WB_isLdrbOff | WB_isStrbOff | WB_isStrbPre | WB_isStrbPost |
                  WB_isLdrOff | WB_isLdrPre | WB_isLdrPost | WB_isCbnz);
    
    always @(posedge clk) begin
        if (print) begin
            $write("%c", WB_vd_vt[7:0]);
        end
        // if (WB_valid & ~WB_was_stalling) begin
        //     if (jmp) begin
        //         $display("	0x%x: branch 0x%x", {WB_pc[29:2], 2'b0}, {((WB_imm[63:2]) + WB_pc), 2'b0});
        //     end else begin
        //         $display("	0x%x: ins 0x%x", {WB_pc[29:2], 2'b0}, mem.data[WB_pc]);
        //     end
        // end

        if (~halt) begin
            WB_valid <= ~WB_flush & (M2_valid | WB_stall);
            WB_pc <= WB_stall ? WB_pc : M2_pc;
            WB_was_stalling <= WB_stall & WB_valid;
            prev_mdata <= X_stall ? mdata : 0;

            if (~WB_stall) begin
                WB_isAdrp <= M2_isAdrp;
                WB_isAddi <= M2_isAddi;
                WB_isMovz <= M2_isMovz;
                WB_isLdrPre <= M2_isLdrPre;
                WB_isLdrPost <= M2_isLdrPost;
                WB_isLdrOff <= M2_isLdrOff;
                WB_isStrbPre <= M2_isStrbPre;
                WB_isStrbPost <= M2_isStrbPost;
                WB_isStrbOff <= M2_isStrbOff;
                WB_isLdrbPre <= M2_isLdrbPre;
                WB_isLdrbPost <= M2_isLdrbPost;
                WB_isLdrbOff <= M2_isLdrbOff;
                WB_isCbnz <= M2_isCbnz;
                WB_vd_vt <= M2_vd_vt;
                WB_rn <= M2_rn;
                WB_rd_rt <= M2_rd_rt;
                WB_sf <= M2_sf;
                WB_sh <= M2_sh;
                WB_hw <= M2_hw;
                WB_size <= M2_size;
                WB_imm <= M2_imm;
                WB_op1 <= M2_op1;
                WB_R1_num_valid <= M2_R1_num_valid & M2_valid;
                WB_R2_num_valid <= M2_R2_num_valid & M2_valid;
                WB_R1_num <= M2_R1_num;
                WB_R2_num <= M2_R2_num;
                WB_M_add_valid <= M2_M_add_valid & M2_valid;
                WB_M_add <= M2_M_add;
            end
        end
            //$display("valid = %d",WB_valid);
            //$display("pc = %d",WB_pc);
    end

    ////////////////////////////
    // Simulation only things //
    ////////////////////////////

    // Simulation only constructs to dump debug information
    initial begin
        $dumpfile("cpu.vcd");
        $dumpvars(0,main);
    end


    // Simulation only constructs to simulate the clock
    reg theClock = 0;

    assign clk = theClock;

    always begin
        #500;
        theClock = !theClock;
    end

endmodule
