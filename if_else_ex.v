wire a = 4;
wire b = 5;

always @(posedge clk) begin
    if (a) begin
        test <= 0;
    end else if (b) begin
        test <= 1;
    end else begin
        test <= 2;
    end
end