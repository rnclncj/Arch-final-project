wire [31:0] D_ins = 143352;
wire D_valid = 1'b1;

wire [3:0] D_ins_num =  D_is_adrp ? 1 :
                            D_is_add ? 2 :
                            D_is_movz ? 3 :
                            D_is_ldr_post ? 4 :
                            D_is_ldr_pre ? 5 :
                            D_is_ldr_unsigned ? 6 :
                            D_is_ldrb_post ? 7 :
                            D_is_ldrb_pre ? 8 :
                            D_is_ldrb_unsigned ? 9 :
                            D_is_strb_post ? 10 : 
                            D_is_strb_pre ? 11 :
                            D_is_strb_unsigned ? 12 :
                            D_is_cbnz ? 13 :
                            0;
    wire D_terminate = D_ins_num == 0;  // is the program at an end state?
    
    wire D_is_adrp = D_valid && D_ins[31] == 1 && D_ins[28:24] == 5'b10000; 
    wire D_is_add = D_valid && D_ins[30:23] == 8'b00100010; 
    wire D_is_movz = D_valid && D_ins[30:23] == 8'b10100101;

    wire D_is_ldr_post = D_valid && D_ins[31] == 1 && D_ins[29:21] == 9'b111000010 && D_ins[11:10] == 2'b01; 
    wire D_is_ldr_pre = D_valid && D_ins[31] == 1 && D_ins[29:21] == 9'b111000010 && D_ins[11:10] == 2'b11;
    wire D_is_ldr_unsigned = D_valid && D_ins[31] == 1 && D_ins[29:22] == 8'b11100101;
    wire D_is_ldr = D_valid && D_is_ldr_post | D_is_ldr_pre | D_is_ldr_unsigned;

    wire D_is_ldrb_post = D_valid && D_ins[31:21] == 11'b00111000010 && D_ins[11:10] == 2'b01;
    wire D_is_ldrb_pre = D_valid && D_ins[31:21] == 11'b00111000010 && D_ins[11:10] == 2'b11;
    wire D_is_ldrb_unsigned = D_valid && D_ins[31:22] == 10'b0011100101;
    wire D_is_ldrb = D_valid && D_is_ldrb_post | D_is_ldrb_pre | D_is_ldrb_unsigned;

    wire D_is_strb_post = D_valid && D_ins[31:21] == 11'b00111000000 && D_ins[11:10] == 2'b01;
    wire D_is_strb_pre = D_valid && D_ins[31:21] == 11'b00111000000 && D_ins[11:10] == 2'b11;
    wire D_is_strb_unsigned = D_valid && D_ins[31:22] == 10'b0011100100;
    wire D_is_strb = D_valid && D_is_strb_post | D_is_strb_pre | D_is_strb_unsigned;

    wire D_is_cbnz = D_valid && D_ins[30:24] == 7'b0110101;

    wire D_is_ldrstrb = D_is_ldr | D_is_ldrb | D_is_strb;
    wire D_is_ldrstrb_unsigned = D_is_ldr_unsigned | D_is_ldrb_unsigned | D_is_strb_unsigned;

    // shared decode
    wire [4:0] D_d = D_ins[4:0];
    wire [4:0] D_n = D_ins[9:5];
    wire D_sf = D_ins[31];
    wire [63:0] D_imm = (D_ins_num == 1) ?  D_adrp_imm :
                        (D_ins_num == 2) ? D_add_imm :
                        (D_ins_num == 3) ? D_movz_imm :
                        0;
    // ldr, ldrb, strb
    wire D_wback = (D_is_ldr_post | D_is_ldr_pre | D_is_ldrb_post | D_is_ldrb_pre | D_is_strb_post | D_is_strb_pre) ? 1 : 0;
    wire D_postindex = (D_is_ldr_post | D_is_ldrb_post | D_is_strb_post) ? 1 : 0;    
    wire [63:0] D_offset =  D_is_ldrstrb ? ( D_is_ldrstrb_unsigned ? D_zero_extend_imm : D_sign_extend_imm ) : 0;
    // ldr only
    wire [1:0] D_scale = D_ins[31:30];
    wire [63:0] D_datasize = 8 << D_scale;
    wire [63:0] D_regsize = (D_scale == 2'b11) ? 64 : 32;

    // adrp
    wire [63:0] D_adrp_imm = {31'b0, D_ins[23:5], D_ins[30:29], 12'b0};

    // add
    wire D_sh = D_ins[22];
    wire [63:0] D_add_imm_is_64 = D_sh ? {40'b0, D_ins[21:10], 12'b0} : {52'b0, D_ins[21:10]};
    wire [63:0] D_add_imm_is_32 = D_sh ? {8'b0, D_ins[21:10], 12'b0} : {40'b0, D_ins[21:10]};
    wire [63:0] D_add_imm = D_sf ? D_add_imm_is_64 : D_add_imm_is_32;

    // movz
    wire [5:0] D_pos = {D_ins[22:21], 4'b0};
    wire [63:0] D_movz_imm = D_ins[20:5];

    // ldr, ldrb, strb       
    wire [63:0] D_sign_extend_imm = { {55{D_ins[20]}} , D_ins[20:12] };
    wire [63:0] D_zero_extend_imm = { 51'b0 , D_ins[21:10] } >> D_scale;

    // cbnz    
    wire [63:0] D_cbnz_offset = { {45{D_ins[23]}} , D_ins[23:5] }; 