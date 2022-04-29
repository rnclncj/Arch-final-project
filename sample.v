`timescale 1ps/1ps

module main();
    wire a = 1;                     // wire a 1 
    wire [7:0] b = 8'b00111000;     // wire b 8'b00111000
    wire [8:0] c = a + b;           // wire c + a b

endmodule
