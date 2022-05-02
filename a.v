always @(posedge clk) begin

        if (F1_valid) begin
            if (WB2_is_flush) begin
                if (F1_valid && WB2_branch_en)
                    F1_pc <= WB2_branch_addr;
                else    
                    // standard flushing behavior
                    F1_pc <= WB2_pc + 1;
            end 
            else if (WB1_is_flush) begin
                if (F1_valid && WB1_branch_en)
                    F1_pc <= WB1_branch_addr;
                else
                    // standard flushing behavior
                    F1_pc <= WB1_pc + 1;
            end 
            else    
                F1_pc <= F1_pc+1;
        end
    end