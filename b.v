reg [31:0]data[0:2047];
always @(posedge clk) begin
    data[waddr] <= wdata;
end