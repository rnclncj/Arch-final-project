wire D_valid;
wire WB2_is_flush;
wire a = 4 +5;

always @(posedge clk) begin
    WB1_valid <= M2_valid & !WB1_is_flush & !WB2_is_flush;
    WB1_pc <= M2_pc;

    if (WB1_valid && WB1_print) begin
        $write("%c", WB1_strb_data);
        WB1_is_flush <= 4;
    end
    WB1_ins_num <= M2_ins_num;
    WB1_d <= M2_d;
    WB1_n <= M2_n;
    WB1_branch_addr <= M2_branch_addr;
    WB1_branch_en <= M2_branch_en;
    WB1_result <= M2_result;
    WB1_memory_reg_wen <= M2_memory_reg_wen;
    WB1_offset_reg_wen <= M2_offset_reg_wen;
    WB1_mem_strb_wen <= M2_mem_strb_wen; 
    WB1_wback <= M2_wback;
    WB1_postindex <= M2_postindex; 
    WB1_offset <= M2_offset;
    WB1_scale <= M2_scale;
    WB1_datasize <= M2_datasize;
    WB1_regsize <= M2_regsize;
    WB1_address <= M2_address;
    WB1_strb_data <= M2_strb_data;
    WB1_print <= M2_print;
    WB1_ins <= M2_ins;
    WB1_ldr_lower <= M2_ldr_lower;
    WB1_terminate <= M2_terminate;    
end