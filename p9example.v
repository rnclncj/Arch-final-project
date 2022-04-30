// ji
wire a = 1; 
wire b = a;
wire c = a;
wire d = ~a;
wire e = a+b+c+d;
wire f = {4,a};
wire g = {4{f,a,b},c,d,e};
assign b = (a == 4) ? c+a : d*~a 
