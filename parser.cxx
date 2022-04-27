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

    public:
    Interpreter(char const* prog): program(prog), current(prog) {}

    string e0() {
        
    }

    // () [] . -> ...
    string e1() {
        if (auto id = consume_identifier()) {
            // TODO: write []
            // read wire value
            return id.value();
        } else if (auto v = consume_literal()) {
            return to_string(v.value());
        } else if (consume("(")) {
            auto v = expression();
            consume(")");
            return v;
        } else {
            fail();
        }
    }

    // ++ -- unary+ unary- ... (Right)
    string e2() {
        return e1();
    }

    // * / % (Left)
    string e3() {
        auto v = e2();

        while (true) {
            if (consume("*")) {
                v = v * e2();
            } else if (consume("/")) {
                auto right = e2();
                v = (right == 0) ? 0 : v / right;
            } else if (consume("%")) {
                auto right = e2();
                v = (right == 0) ? 0 : v % right;
            } else {
                return v;
            }
        }
    }

    // (Left) + -
    string e4() {
        auto v = e3();

        while (true) {
            if (consume("+")) {
                v = v + e3();
            } else if (consume("-")) {
                v = v - e3();
            } else {
                return v;
            }
        }
    }

    // << >>
    string e5() {
        return e4();
    }

    // < <= > >=
    string e6() {
        return e5();
    }

    // == !=
    string e7() {
        return e6();
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

    string expression() {
        return e15();
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