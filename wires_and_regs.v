wire halt = 1;
reg WB_valid;
reg WB_flush = 1;
reg M2_valid = 1;
reg WB_stall = 0;
reg WB_isAdrp;
reg M2_isAdrp = 1;

always @(posedge clk) begin
    if (~halt) begin
        WB_valid <= ~WB_flush & (M2_valid | WB_stall);

        if (~WB_stall) begin
            WB_isAdrp <= M2_isAdrp;
        end
    end
end