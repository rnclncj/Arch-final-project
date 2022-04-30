// libc includes (available in both C and C++)
#include <ctype.h>
#include <fcntl.h>
#include <stddef.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <unistd.h>

// C++ stdlib includes (not available in C)
#include <cmath>
#include <iostream>
#include <optional>
#include <string>
#include <unordered_map>
#include <vector>
using namespace std;

class Interpreter {
    char const* const program;
    char const* current;
    unordered_map<string, pair<int64_t, string>> wire_table{};
    unordered_map<string, string> reg_table{};
    int tempCounter;

    [[noreturn]] void fail() {
        printf("failed at offset %ld\n", size_t(current - program));
        cout << current << endl;
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

    bool peek(const char* str) {
        skip();
        size_t i = 0;
        while (true) {
            char const expected = str[i];
            char const found = *(current + i);
            if (expected == 0) {
                /* survived to the end of the expected string */
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
            char const* start = current;
            do {
                current += 1;
            } while (isalnum(*current) || *current == '[' || *current == ']');
            return ((string)start).substr(0, size_t(current - start));
        } else {
            return {};
        }
    }

    optional<string> consume_literal() {
        skip();

        if (isdigit(*current)) {
            string v = "";
            do {
                v += *current;
                current += 1;
            } while (isalnum(*current) || *current == '\'');
            return v;
        } else {
            return {};
        }
    }

    optional<uint64_t> consume_number() {
        skip();
        if (isdigit(*current)) {
            uint64_t v = 0;
            do {
                v = 10 * v + ((*current) - '0');
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
        if (consume("[")) {
            res += "[";
            while (!consume("]")) {
                res += *current;
            }
            res += "]";
        }
        return res;
    }

    void skip_line() {
        while (*current != '\n') {
            current += 1;
        }
    }

    void skip_block() {
        int counter = 0;
        while (true) {
            if (consume("begin")) {
                counter += 1;
            } else if (consume("end")) {
                counter -= 1;
                if (!counter) {
                    return;
                }
            } else {
                current += 1;
            }
        }
    }

   public:
    Interpreter(char const* prog) : program(prog), current(prog) {
        tempCounter = 0;
    }

    // variable name and optional []
    optional<string> e0() {
        if (auto id = consume_identifier()) {
            return id.value() + consume_bracket();
        } else {
            return {};
        }
    }

    // numbers, ()
    string e1() {
        if (auto e0_ret = e0()) {
            return e0_ret.value();
        } else if (auto v = consume_literal()) {
            return v.value();
        } else if (consume("(")) {
            auto v = expression();
            consume(")");
            return v;
        } else {
            fail();
        }
    }

    // ! ~ & | ~& ~| ^ ~^ ^~ logical negation, negation, reduction AND,
    // reduction OR, reduction NAND, reduction NOR, reduction XOR, reduction
    // XNOR ALL single operand!!
    string e2() {
        // auto v = e1();

        while (true) {
            if (consume("!")) {
                auto right = e2();
                cout << "wire .temp" << to_string(tempCounter) << " ! " << right
                     << endl;
                right = ".temp" + to_string(tempCounter);
                tempCounter += 1;
                return right;
            } else if (consume("~&")) {
                auto right = e2();
                cout << "wire .temp" << to_string(tempCounter) << " ~& "
                     << right << endl;
                right = ".temp" + to_string(tempCounter);
                tempCounter += 1;
                return right;
            } else if (consume("~|")) {
                auto right = e2();
                cout << "wire .temp" << to_string(tempCounter) << " ~| "
                     << right << endl;
                right = ".temp" + to_string(tempCounter);
                tempCounter += 1;
                return right;
            } else if (consume("~^") || consume("^~")) {
                auto right = e2();
                cout << "wire .temp" << to_string(tempCounter) << " ~^ "
                     << right << endl;
                right = ".temp" + to_string(tempCounter);
                tempCounter += 1;
                return right;
            } else if (consume("&")) {
                auto right = e2();
                cout << "wire .temp" << to_string(tempCounter) << " & " << right
                     << endl;
                right = ".temp" + to_string(tempCounter);
                tempCounter += 1;
                return right;
            } else if (consume("|")) {
                auto right = e2();
                cout << "wire .temp" << to_string(tempCounter) << " | " << right
                     << endl;
                right = ".temp" + to_string(tempCounter);
                tempCounter += 1;
                return right;
            } else if (consume("^")) {
                auto right = e2();
                cout << "wire .temp" << to_string(tempCounter) << " ^ " << right
                     << endl;
                right = ".temp" + to_string(tempCounter);
                tempCounter += 1;
                return right;
            } else if (consume("~")) {
                auto right = e2();
                cout << "wire .temp" << to_string(tempCounter) << " ~ " << right
                     << endl;
                right = ".temp" + to_string(tempCounter);
                tempCounter += 1;
                return right;
            } else {
                return e1();
            }
        }
    }

    // + - unary, sign, single operand
    string e3() {
        if (consume("+")) {
            auto right = e3();
            cout << "wire .temp" << to_string(tempCounter) << " + " << right
                 << endl;
            right = ".temp" + to_string(tempCounter);
            tempCounter += 1;
            return right;
        } else if (consume("-")) {
            auto right = e3();
            cout << "wire .temp" << to_string(tempCounter) << " - " << right
                 << endl;
            right = ".temp" + to_string(tempCounter);
            tempCounter += 1;
            return right;
        } else {
            return e2();
        }
    }

    // {} {{}} concatenation, possibly replication
    string e4() {
        if (consume("{")) {
            vector<string> operators;
            string res = ".temp" + to_string(tempCounter);
            tempCounter += 1;
            do {
                // either literal values or replication
                if (auto id = consume_literal()) {
                    auto v = id.value();
                    if (peek("{")) {
                        // run it again to get the inside
                        auto inside = e4();
                        cout << "wire .temp" << to_string(tempCounter)
                             << " {{}} " << v << " " << inside << endl;
                        v = ".temp" + to_string(tempCounter);
                        tempCounter += 1;
                        operators.push_back(v);
                    } else {
                        operators.push_back(v);
                    }
                } else if (auto id = consume_identifier()) {
                    operators.push_back(id.value());
                }
            } while (consume(","));
            consume("}");
            cout << "wire " << res << " {}";
            for (auto a : operators) {
                cout << " " << a;
            }
            cout << endl;
            return res;
        } else {
            return e3();
        }
    }

    // * / %
    string e5() {
        auto v = e4();
        while (true) {
            if (consume("*")) {
                auto right = e4();
                cout << "wire .temp" << to_string(tempCounter) << " * " << v
                     << " " << right << endl;
                v = ".temp" + to_string(tempCounter);
                tempCounter += 1;
            } else if (consume("/")) {
                auto right = e4();
                cout << "wire .temp" << to_string(tempCounter) << " / " << v
                     << " " << right << endl;
                v = ".temp" + to_string(tempCounter);
                tempCounter += 1;
            } else if (consume("%")) {
                auto right = e4();
                cout << "wire .temp" << to_string(tempCounter) << " % " << v
                     << " " << right << endl;
                v = ".temp" + to_string(tempCounter);
                tempCounter += 1;
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
                cout << "wire .temp" << to_string(tempCounter) << " + " << v
                     << " " << right << endl;
                v = ".temp" + to_string(tempCounter);
                tempCounter += 1;
            } else if (consume("-")) {
                auto right = e5();
                cout << "wire .temp" << to_string(tempCounter) << " - " << v
                     << " " << right << endl;
                v = ".temp" + to_string(tempCounter);
                tempCounter += 1;
            } else {
                return v;
            }
        }
    }

    // << >>
    string e7() {
        auto v = e6();
        while (true) {
            if (consume("<<")) {
                auto right = e6();
                cout << "wire .temp" << to_string(tempCounter) << " << " << v
                     << " " << right << endl;
                v = ".temp" + to_string(tempCounter);
                tempCounter += 1;
            } else if (consume(">>")) {
                auto right = e6();
                cout << "wire .temp" << to_string(tempCounter) << " >> " << v
                     << " " << right << endl;
                v = ".temp" + to_string(tempCounter);
                tempCounter += 1;
            } else {
                return v;
            }
        }
    }

    // > >= < <=
    string e8() {
        auto v = e7();
        while (true) {
            if (consume(">=")) {
                auto right = e7();
                cout << "wire .temp" << to_string(tempCounter) << " >= " << v
                     << " " << right << endl;
                v = ".temp" + to_string(tempCounter);
                tempCounter += 1;
            } else if (consume("<=")) {
                auto right = e7();
                cout << "wire .temp" << to_string(tempCounter) << " <=> " << v
                     << " " << right << endl;
                v = ".temp" + to_string(tempCounter);
                tempCounter += 1;
            } else if (consume("<")) {
                auto right = e7();
                cout << "wire .temp" << to_string(tempCounter) << " < " << v
                     << " " << right << endl;
                v = ".temp" + to_string(tempCounter);
                tempCounter += 1;
            } else if (consume(">")) {
                auto right = e7();
                cout << "wire .temp" << to_string(tempCounter) << " > " << v
                     << " " << right << endl;
                v = ".temp" + to_string(tempCounter);
                tempCounter += 1;
            } else {
                return v;
            }
        }
    }

    // == !=
    string e9() {
        auto v = e8();
        while (true) {
            if (consume("==")) {
                auto right = e8();
                cout << "wire .temp" << to_string(tempCounter) << " == " << v
                     << " " << right << endl;
                v = ".temp" + to_string(tempCounter);
                tempCounter += 1;
            } else if (consume("!=")) {
                auto right = e8();
                cout << "wire .temp" << to_string(tempCounter) << " != " << v
                     << " " << right << endl;
                v = ".temp" + to_string(tempCounter);
                tempCounter += 1;
            } else {
                return v;
            }
        }
    }

    // === !==
    string e10() {
        auto v = e9();

        while (true) {
            if (consume("===")) {
                auto right = e9();
                cout << "wire .temp" << to_string(tempCounter) << " === " << v
                     << " " << right << endl;
                v = ".temp" + to_string(tempCounter);
                tempCounter += 1;
            } else if (consume("!==")) {
                auto right = e9();
                cout << "wire .temp" << to_string(tempCounter) << " !== " << v
                     << " " << right << endl;
                v = ".temp" + to_string(tempCounter);
                tempCounter += 1;
            } else {
                return v;
            }
        }
    }

    // &
    string e11() {
        auto v = e10();

        while (true) {
            if (consume("&") && !peek("&")) {
                auto right = e10();
                cout << "wire .temp" << to_string(tempCounter) << " & " << v
                     << " " << right << endl;
                v = ".temp" + to_string(tempCounter);
                tempCounter += 1;
            } else {
                return v;
            }
        }
    }

    // ^, ^~, ~^
    string e12() {
        auto v = e11();

        while (true) {
            if (consume("^~") || consume("~^")) {
                auto right = e11();
                cout << "wire .temp" << to_string(tempCounter) << " ~^ " << v
                     << " " << right << endl;
                v = ".temp" + to_string(tempCounter);
                tempCounter += 1;
            } else if (consume("^")) {
                auto right = e11();
                cout << "wire .temp" << to_string(tempCounter) << " ^ " << v
                     << " " << right << endl;
                v = ".temp" + to_string(tempCounter);
                tempCounter += 1;
            } else {
                return v;
            }
        }
    }

    // |
    string e13() {
        auto v = e12();

        while (true) {
            if (consume("|") && !peek("|")) {
                auto right = e12();
                cout << "wire .temp" << to_string(tempCounter) << " | " << v
                     << " " << right << endl;
                v = ".temp" + to_string(tempCounter);
                tempCounter += 1;
            } else {
                return v;
            }
        }
    }

    // &&
    string e14() {
        auto v = e13();

        while (true) {
            if (consume("&&")) {
                auto right = e13();
                cout << "wire .temp" << to_string(tempCounter) << " && " << v
                     << " " << right << endl;
                v = ".temp" + to_string(tempCounter);
                tempCounter += 1;
            } else {
                return v;
            }
        }
    }

    // ||
    string e15() {
        auto v = e14();

        while (true) {
            if (consume("||")) {
                auto right = e14();
                cout << "wire .temp" << to_string(tempCounter) << " || " << v
                     << " " << right << endl;
                v = ".temp" + to_string(tempCounter);
                tempCounter += 1;
            } else {
                return v;
            }
        }
    }

    // ? :
    string e16() {
        auto v = e15();

        // make everything ternaries
        while (true) {
            if (consume("?")) {
                auto first = e14();
                if (consume(":")) {
                    auto second = e14();
                    cout << "wire .temp" << to_string(tempCounter)
                         << " ?: " << v << " " << first << " " << second
                         << endl;
                    v = ".temp" + to_string(tempCounter);
                    tempCounter += 1;
                }
            } else {
                return v;
            }
        }
    }

    string expression() { return e16(); }

    int64_t get_size() {
        int64_t size;
        if (consume("[")) {
            size = consume_number().value();
            consume(":");
            size -= consume_number().value();
            size = abs(size);
            consume("]");
            return size;
        }
        return 1;
    }

    bool statement() {
        string wire_name;
        string reg_name;
        int64_t num_bits;
        // int64_t size;

        if (consume("wire")) {
            num_bits = get_size();
            wire_name = consume_identifier().value();
            if (consume("=")) {
                string wire_inputs = expression();
                // printf("wire [%ld]%s ", num_bits, wire_name.c_str());
                cout << "wire " << wire_name << " ";
                // parse expression
                wire_table[wire_name] = make_pair(num_bits, wire_inputs);
                cout << wire_inputs;
            } else {
                wire_table[wire_name] = make_pair(num_bits, "");
            }
            printf("\n");
            return true;

        } else if (consume("assign")) {
            wire_name = consume_identifier().value();

            consume("=");
            num_bits = wire_table[wire_name].first;
            printf("wire [%ld]%s ", num_bits, wire_name.c_str());
            // parse expression
            string wire_inputs = expression();
            wire_table[wire_name] = make_pair(num_bits, wire_inputs);
            cout << wire_inputs << endl;
            return true;
        } else if (consume("reg")) {
            num_bits = get_size();
            reg_name = consume_identifier().value();
            return true;
            // TODO: add to map, skip rest of the line, only initialization for
            // reg, maybe save for display
        } else if (consume("always @(posedge clk)")) {
            if (consume("$")) {
                skip_line();
                return true;
            } else if (consume("if")) {
                return true;
            } else if (consume("for")) {
                // not doing for now
                return true;
            } else {
                if (auto id = consume_identifier()) {
                    reg_name = id.value();
                    consume("<=");
                    string reg_inputs = expression();
                    reg_table[reg_name] = reg_inputs;
                    return true;
                }
            }
        } else if (consume("initial")) {
            skip_block();
            return true;
        } else if (consume("//")) {
            skip_line();
            return true;
        } else {
            return false;
        }

        return false;
    }

    void statements() {
        while (statement()) {
            consume(";");
            if (consume("//")) {
                skip_line();
            }
        }
    }

    void run() {
        statements();
        // end_or_fail();
    }
};

int main(int argc, const char* const* const argv) {
    if (argc != 2) {
        fprintf(stderr, "usage: %s <file name>\n", argv[0]);
        exit(1);
    }

    // open the file
    int fd = open(argv[1], O_RDONLY);
    if (fd < 0) {
        perror("open");
        exit(1);
    }

    // determine its size (std::filesystem::get_size?)
    struct stat file_stats;
    int rc = fstat(fd, &file_stats);
    if (rc != 0) {
        perror("fstat");
        exit(1);
    }

    // map the file in my address space
    char const* prog =
        (char const*)mmap(0, file_stats.st_size, PROT_READ, MAP_PRIVATE, fd, 0);
    if (prog == MAP_FAILED) {
        perror("mmap");
        exit(1);
    }

    Interpreter x{prog};
    x.run();
    return 0;
}
