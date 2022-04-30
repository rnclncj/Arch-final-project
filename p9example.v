// setup
wire a = 2'b10; 
wire b;
wire c;
// testing operators
wire d = a[1:0];
wire e = (a+b)/(c/d);
wire f = !!a;
wire g = ~c;

// wire g = {4{f,a,b},c,d,e};
// assign b = (a == 4) ? c+a : d*~a 
