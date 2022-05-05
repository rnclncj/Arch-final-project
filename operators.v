// checking =
wire a = 5'b10110;
// checking [:]
wire b = a[3:2];
// checking ()
wire c = (a + 4) / b;
// checking !, ~, &, |, ~&, ~|, ^, ~^, ^~
wire d = (!a + ~b) - &(c + |b) + ~&a + ~|c / ^b + ~^a + ^~b;
// checking unary +/-
wire e = +a / -b;
// checking {}, {{}}
wire f = {43{b[3:0]}, 4'b100};
// checking *, /, %
wire g = a * b / (c % d);
// checking binary +/-
wire h = a + b - c;
// checking <<, >>
wire i = b >> 4 << c;
// checking inequalities
wire j = (a >= b) ? (a > b ? c : 3'h803) : ((a <= b) ? (a < b ? c : 3'h327) : 0);
// checking ==, !=
wire k = (a == b) | (a != c);
// checking ===, !==
wire l = (a === b) == (a !== c);
// checking &
wire m = a & b;
// checking ^, ~^, ^~
wire n = a ^ b  + ^a ~^ c - c ^~ a;
// checking |
wire o = a | b;
// checking &&
wire p = a && (b & &c);
// checking ||
wire q = a | (b || ~|c);
// checking ?:
wire r = a ? b : c;