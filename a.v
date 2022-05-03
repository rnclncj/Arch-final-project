always @(posedge clk) begin
    if (F1_valid && WB2_branch_en)
        F1_pc <= WB2_branch_addr;
    else    
        // standard flushing behavior
        F1_pc <= WB2_pc + 1;
end