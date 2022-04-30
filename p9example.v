// setup
// wire x = 2'b10; 
// wire y = 5;
// wire z;
// testing operators
// wire a = s0[1:0];
// wire b = (x+y)/(x/z);
// wire c1 = !!x;
// wire c2 = ~!~y;
// wire c3 = &y;
// wire c4 = |c2;
// assign c4 = ~|c2 && ~&c3;
// wire c4 = x + y;
// wire c5 = ^c4 + ~^y*x / (^~x || ~^y);
// wire d1 = +3;
// wire d2 = x*-y;
// wire ef = {4{f,a,b},c,d,e};
// wire g = ef % 4'b0101;
// wire h1 = 2 + 3 - ef;
// wire h2 = 4'b0101/4 - ef;
// wire i = h2 << 4+x >> y*ef >> y;
// assign i = (a >= 4 && a <= 5 || a > 7 & x < 3) ? c+a : d*~a 
// wire j = x == y || y != z;
// assign j = x === y || y !== z;
// wire k = a + &b - a & b;
// wire l = ((a ^ b) ^~ c || x) ~^ b;
wire m = a | b;
wire n = a && b;
wire o = a || b;

