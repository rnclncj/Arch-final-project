wire D_valid;
wire WB2_is_flush;
wire a = 4 +5;
// wire b = a * 6;
// wire c = a * (^~a) /6;

always @(posedge clk) begin
    if (a == -63) 
        D_valid <= WB2_is_flush;
    else if (a == 4) 
        D_valid <= !WB2_is_flush;
    else 
        D_valid <= 4 + WB2_is_flush;
    D_pc <= F2_pc;
end