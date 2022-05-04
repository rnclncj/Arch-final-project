wire halt;
reg WB_valid;
reg WB_flush;
reg M2_valid;
reg WB_stall;
reg WB_stall;
reg WB_isAdrp;
reg M2_isAdrp;

always @(posedge clk) begin
    if (~halt) begin
            WB_valid <= ~WB_flush & (M2_valid | WB_stall);

        if (~WB_stall) begin
            WB_isAdrp <= M2_isAdrp;
        end
    end
end