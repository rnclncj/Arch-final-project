wire D_valid;
wire WB2_is_flush;
wire a = 4 +5;

always @(posedge clk)
    if (a == -63) 
        D_valid <= WB2_is_flush
    else 
        D_valid <= 4 + WB2_is_flush;
    