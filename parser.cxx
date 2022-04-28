    // libc includes (available in both C and C++)
    #include <sys/mman.h>
    #include <sys/stat.h>
    #include <unistd.h>
    #include <fcntl.h>
    #include <stdlib.h>
    #include <ctype.h>
    #include <stdio.h>
    #include <stdint.h>

    // C++ stdlib includes (not available in C)
    #include <optional>
    #include <iostream>
    #include <string>
    #include <unordered_map>
    #include <cmath>
    using namespace std;

    // Implementation includes

    class Interpreter {
        char const * const program;
        char const * current;
        unordered_map<string,pair<int64_t, string>> wire_table{};
        unordered_map<string,string> reg_table{};
        int tempCounter;

    [[noreturn]]
    void fail() {
        printf("failed at offset %ld\n",size_t(current-program));
        printf("%s\n",current);
        exit(1);
    }

    void end_or_fail() {
        while (isspace(*current)) {
            current += 1;
        }
        if (*current != 0) fail();
    }

    void consume_or_fail(const char* str) {
        if (!consume(str)) {
            fail();
        }
    }

    void skip() {
        while (isspace(*current)) {
            current += 1;
        }
    }

    bool consume(const char* str) {
        skip();

        size_t i = 0;
        while (true) {
            char const expected = str[i];
            char const found = current[i];
            if (expected == 0) {
            /* survived to the end of the expected string */
                current += i;
                return true;
            }
            if (expected != found) {
                return false;
            }
            // assertion: found != 0
            i += 1;
        }

    }

    optional<string> consume_identifier() {
        skip();

        if (isalpha(*current)) {
            char const * start = current;
            do {
            current += 1;
            } while(isalnum(*current));
            return ((string)start).substr(0,size_t(current-start));
        } else {
            return {};
        }
    }

    optional<uint64_t> consume_literal() {
        skip();

        if (isdigit(*current)) {
            uint64_t v = 0;
            do {
            v = 10*v + ((*current) - '0');
            current += 1;
            } while (isdigit(*current));
            return v;
        } else {
            return {};
        }
    }

    string consume_bracket() {
        skip();
        string res = "";
        if(consume("[")) {
            res += "[";
            while (!consume("]")) {
                res += *current;
            }
            res += "]";
        }
        return res;
    }

    public:
    Interpreter(char const* prog): program(prog), current(prog) {}

    // []
    string e0() {
        if (auto id = consume_identifier()) {
            return id.value() + consume_bracket();
        }
    }

    // ()
    string e1() {
        auto v = e0();
        
        if (auto v = consume_literal()) {
            return to_string(v.value());
        } else if (consume("(")) {
            auto v = expression();
            consume(")");
            return v;
        } else {
            fail();
        }
    }

    // ! ~ & | ~& ~| ^ ~^ ^~ logical negation, negation, reduction AND, reduction OR, reduction NAND, reduction NOR, reduction XOR, reduction XNOR
    string e2() {
        auto v = e1();

        while(true) {
            if (consume("!")) {

            } else if (consume("~&")) {

            } else if (consume("~|")) {

            } else if (consume("~^") || consume("^~")) {

            } else if (consume("&")) {
                
            } else if (consume("|")) {

            } else if (consume("^")) {

            } else if (consume("~")) {
                
            } else {
                return v;
            }
        }
    }

    // + - unary, sign
    string e3() {
        auto v = e2();
        return v;
    }

    // {} {{}} concatenation, possibly replication
    string e4() {
        // TODO
        auto v = e3();
        return v;
    }

    // * / %
    string e5() {
        auto v = e4();
        while (true) {
            if (consume("*")) {

            } else if (consume("/")) {

            } else if (consume("%")) {
                
            } else {
                return v;
            }
        }
    }

    // + - binary
    string e6() {
        auto v = e5();

        while (true) {
            if (consume("+")) {
                auto right = e5();
            } else if (consume("-")) {
                auto right = e5();
            } else {
                return v;
            }
        }
    }

    // << >>
    string e7() {
        auto v = e6();
        while(true) {
            if ()
        }
    }

    // (left) &
    string e8() {
        return e7();
    }

    // ^
    string e9() {
        return e8();
    }

    // |
    string e10() {
        return e9();
    }

    // &&
    string e11() {
        return e10();
    }

    // ||
    string e12() {
        return e11();
    }

    // (right with special treatment for middle expression) ?:
    string e13() {
        return e12();
    }

    // = += -= ...
    string e14() {
        return e13();
    }

    // ,
    string e15() {
        return e14();
    }

    // ,
    string e16() {
        return e15();
    }

    // ||
    string e17() {
        
        return e16();
    }

    // ? :
    string e18() {
        auto v = e18();

        // make everything ternaries
        while (true) {
            if (consume("?")) {
                if (consume(":")) {
                    
                }
            }   
            else {
                return v;
            }
    }

    string expression() {
        return e18();
    }

    int64_t get_size() {
        int64_t size;
        if (consume("[")) {
            size = consume_literal().value();
            consume(":");
            size -= consume_literal().value();
            size = abs(size);
            consume("]");
            return size;
        }
        return 1;
    }

    void statement() {
        string wire_name;
        string reg_name;
        int64_t num_bits;
        int64_t size;

        if (consume("wire")) {
            num_bits = get_size();
            wire_name = consume_identifier().value();
            if (consume("=")) {
                printf("wire [%d]%s ", num_bits, wire_name);
                // parse expression
                string wire_inputs = expression();
                wire_table[wire_name] = make_pair(num_bits, wire_inputs);
                printf("%s", wire_inputs);
            } else {
                wire_table[wire_name] = make_pair(num_bits, "");
            }
            printf("\n");   

        } else if (consume("assign")) {
            wire_name = consume_identifier().value();
            
            consume("=");
            num_bits = wire_table[wire_name].first;
            printf("wire [%d]%s ", num_bits, wire_name);
            // parse expression
            string wire_inputs = expression();
            wire_table[wire_name] = make_pair(num_bits, wire_inputs);
            printf("%s\n", wire_inputs);       
        } else if (consume("reg")) {
            num_bits = get_size();
            reg_name = consume_identifier().value();
            // TODO: add to map, skip rest of the line
        } else if (consume("always @(posedge clk)")) {
            if (consume("initial")) {
                // TODO: skip entire block
            } else if (consume("$")) {
                // TODO: skip line
            } else if (consume("if")) {

            } else if (consume("for")) {
                // not doing for now
            } else if (auto id = consume_identifier()) {
                reg_name = id.value();
                consume("<=");
                string reg_inputs = expression();
                reg_table[reg_name] = reg_inputs;
            }
        } else {
            fail();
        }


    void statements() {
        while (statement());
    }

    void run() {
        statements();
        end_or_fail();
    }
    };


    int main(int argc, const char *const *const argv) {

    if (argc != 2) {
        fprintf(stderr,"usage: %s <file name>\n",argv[0]);
        exit(1);
    }

    // open the file
    int fd = open(argv[1],O_RDONLY);
    if (fd < 0) {
        perror("open");
        exit(1);
    }

    // determine its size (std::filesystem::get_size?)
    struct stat file_stats;
    int rc = fstat(fd,&file_stats);
    if (rc != 0) {
        perror("fstat");
        exit(1);
    }

    // map the file in my address space
    char const* prog = (char const *)mmap(
        0,
        file_stats.st_size,
        PROT_READ,
        MAP_PRIVATE,
        fd,
        0);
    if (prog == MAP_FAILED) {
        perror("mmap");
        exit(1);
    }

    Interpreter x{prog};


    x.run();


    return 0;
    }

    // vim: tabstop=4 shiftwidth=2 expandtab