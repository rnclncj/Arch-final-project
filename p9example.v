reg a = 4 + 6;
reg D_valid;
reg F2_valid;
reg WB1_is_flush;
reg WB2_is_flush;
always @(posedge clk) begin
    if (a == 10) begin
        D_valid <= 4;
    else begin
        D_valid <= 6;
    end
    D_pc <= F2_pc;
end