wire D_valid;
wire WB2_is_flush;
wire a = 4 +5;

always @(posedge clk) begin
    if (a == -63) begin
        D_valid <= WB2_is_flush;wire D_valid;
wire WB2_is_flush;
wire a = 4 +5;

always @(posedge clk) begin
    if (a == -63) begin
        D_valid <= WB2_is_flush;
    end
    else begin
        D_valid <= !WB2_is_flush;
    end
    D_pc <= F2_pc;
end
    end
    else begin
        D_valid <= !WB2_is_flush;
    end
    D_pc <= F2_pc;
end