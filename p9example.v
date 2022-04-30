wire a = 4 + /* 5 - 3 * 18 */ 6
always @(posedge clk) begin
    D_valid <= F2_valid & !WB1_is_flush & !WB2_is_flush;
    D_pc <= F2_pc;
end