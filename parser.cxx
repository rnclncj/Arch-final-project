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
#include <tuple>
#include <unordered_map>
#include <vector>
using namespace std;

class Interpreter {
    char const* const program;
    char const* current;

    vector<string> module_names{};
    // list of modules input/outputs and param names
    vector<vector<pair<bool, string>>> module_list{};
    unordered_map<string, pair<int64_t, string>> wire_table{};
    unordered_map<string, string> reg_table{};

    int tempCounter;
    int timeout = 1024;

    [[noreturn]] void fail() {
        printf("failed at offset %ld\n", size_t(current - program));
        cout << current << endl;
        exit(1);
    }

    void end_or_fail() {
        while (isspace(*current)) {
            current += 1;
        }
        if (*current != 0) {
            cout << "current: " << *current << endl;
            fail();
        }
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

        if (isalpha(*current) || *current == '_') {
            char const* start = current;
            do {
                current += 1;
            } while (isalnum(*current) || *current == '_');
            return ((string)start).substr(0, size_t(current - start));
        } else {
            return {};
        }
    }

    optional<string> consume_identifier_with_brackets() {
        skip();

        if (isalpha(*current) || *current == '_') {
            char const* start = current;
            do {
                current += 1;
            } while (isalnum(*current) || *current == '_' || *current == '[' ||
                     *current == ']' || *current == ':');
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

    optional<uint32_t> consume_number() {
        skip();
        if (isdigit(*current)) {
            uint32_t v = 0;
            do {
                v = 10 * v + ((*current) - '0');
                current += 1;
            } while (isdigit(*current));
            return v;
        } else {
            return {};
        }
    }

    optional<string> consume_bit_select() {
        skip();
        if (consume("[")) {
            string res = "[";
            while (*current != ']') {
                res += *current;
                current += 1;
            }
            consume("]");
            res += "]";
            return res;
        }
        return {};
    }

    void skip_line() {
        while (*current != '\n') {
            current += 1;
        }
    }

    void skip_multiline() {
        while (!consume("*/")) {
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

    string next_temp_wire() { return ".temp" + to_string(tempCounter++); }

   public:
    Interpreter(char const* prog) : program(prog), current(prog) {
        tempCounter = 0;
    }

    // variable name and optional []
    optional<string> e0() {
        // parse multiline comments
        if (peek("/*")) {
            skip_multiline();
        }
        if (auto id = consume_identifier()) {
            if (auto bit_select = consume_bit_select()) {
                string res = ".temp" + to_string(tempCounter++);
                cout << "wire " << res << " " << bit_select.value() << " "
                     << id.value() << endl;
                return res;
            }
            return id.value();
        } else {
            return {};
        }
    }

    // numbers, ()
    string e1() {
        if (auto e0_ret = e0()) {
            return e0_ret.value();
        } else if (auto v = consume_literal()) {
            string res = ".temp" + to_string(tempCounter++);
            cout << "wire " << res << " -- " << v.value() << endl;
            return res;
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
        while (true) {
            if (consume("!")) {
                auto right = e2();
                cout << "wire .temp" << to_string(tempCounter) << " ! " << right
                     << endl;
                right = ".temp" + to_string(tempCounter++);
                return right;
            } else if (consume("~&")) {
                auto right = e2();
                cout << "wire .temp" << to_string(tempCounter) << " ~& "
                     << right << endl;
                right = ".temp" + to_string(tempCounter++);
                return right;
            } else if (consume("~|")) {
                auto right = e2();
                cout << "wire .temp" << to_string(tempCounter) << " ~| "
                     << right << endl;
                right = ".temp" + to_string(tempCounter++);
                return right;
            } else if (consume("~^") || consume("^~")) {
                auto right = e2();
                cout << "wire .temp" << to_string(tempCounter) << " ~^ "
                     << right << endl;
                right = ".temp" + to_string(tempCounter++);
                return right;
            } else if (consume("&")) {
                auto right = e2();
                cout << "wire .temp" << to_string(tempCounter) << " & " << right
                     << endl;
                right = ".temp" + to_string(tempCounter++);
                return right;
            } else if (consume("|")) {
                auto right = e2();
                cout << "wire .temp" << to_string(tempCounter) << " | " << right
                     << endl;
                right = ".temp" + to_string(tempCounter++);
                return right;
            } else if (consume("^")) {
                auto right = e2();
                cout << "wire .temp" << to_string(tempCounter) << " ^ " << right
                     << endl;
                right = ".temp" + to_string(tempCounter++);
                return right;
            } else if (consume("~")) {
                auto right = e2();
                cout << "wire .temp" << to_string(tempCounter) << " ~ " << right
                     << endl;
                right = ".temp" + to_string(tempCounter++);
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
            right = ".temp" + to_string(tempCounter++);
            return right;
        } else if (consume("-")) {
            auto right = e3();
            cout << "wire .temp" << to_string(tempCounter) << " - " << right
                 << endl;
            right = ".temp" + to_string(tempCounter++);
            return right;
        } else {
            return e2();
        }
    }

    // {} {{}} concatenation, possibly replication
    string e4() {
        if (consume("{")) {
            vector<string> operators;
            string res = ".temp" + to_string(tempCounter++);
            do {
                // either literal values or replication
                if (auto id = consume_literal()) {
                    string res = next_temp_wire();
                    cout << "wire " << res << " -- " << id.value() << endl;
                    auto v = res;
                    if (peek("{")) {
                        // run it again to get the inside
                        auto inside = e4();
                        cout << "wire .temp" << to_string(tempCounter)
                             << " {{}} " << v << " " << inside << endl;
                        v = ".temp" + to_string(tempCounter++);
                        operators.push_back(v);
                    } else {
                        operators.push_back(v);
                    }
                } else {
                    operators.push_back(e4());
                }
                // else if (auto id = consume_identifier_with_brackets()) {
                //     operators.push_back(id.value());
                // }
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
                v = ".temp" + to_string(tempCounter++);
            } else if (consume("/")) {
                auto right = e4();
                cout << "wire .temp" << to_string(tempCounter) << " / " << v
                     << " " << right << endl;
                v = ".temp" + to_string(tempCounter++);
            } else if (consume("%")) {
                auto right = e4();
                cout << "wire .temp" << to_string(tempCounter) << " % " << v
                     << " " << right << endl;
                v = ".temp" + to_string(tempCounter++);
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
                v = ".temp" + to_string(tempCounter++);
            } else if (consume("-")) {
                auto right = e5();
                cout << "wire .temp" << to_string(tempCounter) << " - " << v
                     << " " << right << endl;
                v = ".temp" + to_string(tempCounter++);
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
                v = ".temp" + to_string(tempCounter++);
            } else if (consume(">>")) {
                auto right = e6();
                cout << "wire .temp" << to_string(tempCounter) << " >> " << v
                     << " " << right << endl;
                v = ".temp" + to_string(tempCounter++);
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
                v = ".temp" + to_string(tempCounter++);
            } else if (consume("<=")) {
                auto right = e7();
                cout << "wire .temp" << to_string(tempCounter) << " <= " << v
                     << " " << right << endl;
                v = ".temp" + to_string(tempCounter++);
            } else if (consume("<")) {
                auto right = e7();
                cout << "wire .temp" << to_string(tempCounter) << " < " << v
                     << " " << right << endl;
                v = ".temp" + to_string(tempCounter++);
            } else if (consume(">")) {
                auto right = e7();
                cout << "wire .temp" << to_string(tempCounter) << " > " << v
                     << " " << right << endl;
                v = ".temp" + to_string(tempCounter++);
            } else {
                return v;
            }
        }
    }

    // == !=
    string e9() {
        auto v = e8();

        while (true) {
            if (!peek("===") && consume("==")) {
                auto right = e8();
                cout << "wire .temp" << to_string(tempCounter) << " == " << v
                     << " " << right << endl;
                v = ".temp" + to_string(tempCounter++);
            } else if (!peek("!==") && consume("!=")) {
                auto right = e8();
                cout << "wire .temp" << to_string(tempCounter) << " != " << v
                     << " " << right << endl;
                v = ".temp" + to_string(tempCounter++);
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
                v = ".temp" + to_string(tempCounter++);
            } else if (consume("!==")) {
                auto right = e9();
                cout << "wire .temp" << to_string(tempCounter) << " !== " << v
                     << " " << right << endl;
                v = ".temp" + to_string(tempCounter++);
            } else {
                return v;
            }
        }
    }

    // &
    string e11() {
        auto v = e10();

        while (true) {
            if (!peek("&&") && consume("&")) {
                auto right = e10();
                cout << "wire .temp" << to_string(tempCounter) << " & " << v
                     << " " << right << endl;
                v = ".temp" + to_string(tempCounter++);
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
                v = ".temp" + to_string(tempCounter++);
            } else if (consume("^")) {
                auto right = e11();
                cout << "wire .temp" << to_string(tempCounter) << " ^ " << v
                     << " " << right << endl;
                v = ".temp" + to_string(tempCounter++);
            } else {
                return v;
            }
        }
    }

    // |
    string e13() {
        auto v = e12();

        while (true) {
            if (!peek("||") && consume("|")) {
                auto right = e12();
                cout << "wire .temp" << to_string(tempCounter) << " | " << v
                     << " " << right << endl;
                v = ".temp" + to_string(tempCounter++);
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
                v = ".temp" + to_string(tempCounter++);
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
                v = ".temp" + to_string(tempCounter++);
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
                    v = ".temp" + to_string(tempCounter++);
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

    // assign -> input
    void update_modules(string name) {
        // for each module
        for (uint32_t i = 0; i < module_names.size(); i++) {
            for (uint32_t j = 0; j < module_list[i].size(); j++) {
                // inputs are associated with "assign" keyword in Verilog
                if (module_list[i][j].second == name) {
                    module_list[i][j].first = true;
                }
            }
        }
    }

    bool statement() {
        string wire_name;
        string reg_name;
        int64_t num_bits;
        if (peek("/*")) {
            skip_multiline();
            return true;
        } else if (consume("wire")) {
            num_bits = get_size();
            wire_name = consume_identifier().value();
            if (consume("=")) {
                string wire_inputs = expression();
                cout << "wire [" << num_bits << "]" << wire_name << " = "
                     << wire_inputs << endl;
                wire_table[wire_name] = make_pair(num_bits, wire_inputs);
            } else {
                wire_table[wire_name] = make_pair(num_bits, "");
            }
            return true;
        } else if (consume("assign")) {
            num_bits = get_size();
            wire_name = consume_identifier().value();
            update_modules(wire_name);
            consume("=");
            // parse expression
            string wire_inputs = expression();
            wire_table[wire_name] = make_pair(num_bits, wire_inputs);
            cout << "wire [" << num_bits << "]" << wire_name << " = "
                 << wire_inputs << endl;
            return true;
            // peek skip whitespaces (doesn't read!)
        } else if (consume("reg") && (*current == ' ' || peek("["))) {
            num_bits = get_size();
            reg_name = consume_identifier().value();
            reg_table[reg_name] = reg_name;
            skip_line();
            return true;
        } else if (consume("always @(") && consume("posedge clk") &&
                   consume(")")) {
            // TODO: expand parameters
            unordered_map<string, string> res;
            if (peek("begin")) {
                res = always_statements();
            } else {
                res = always_statement();
                consume(";");
            }
            for (auto i = res.begin(); i != res.end(); i++) {
                reg_table[i->first] = i->second;
            }
            return true;
        } else if (consume("initial") || consume("for") || consume("if") ||
                   (peek("always begin") && consume("always"))) {
            skip_block();
            return true;
        } else if (consume("//") || consume("$") || consume("`") ||
                   consume("module") || consume("endmodule")) {
            skip_line();
            return true;
        } else if (auto id = consume_identifier()) {
            string module_name = consume_identifier().value();
            vector<pair<bool, string>> params;
            consume("(");
            do {
                string param = consume_identifier().value();
                // by default output
                params.push_back(make_pair(false, param));
            } while (consume(","));
            consume(")");
            module_names.push_back(module_name);
            module_list.push_back(params);
            return true;
        } else {
            return false;
        }
        return false;
    }

    unordered_map<string, string> always_statement() {
        string reg_name;
        unordered_map<string, string> res;
        string prev_condition;
        string condition;
        if (peek("/*")) {
            skip_multiline();
            return always_statement();
        } else if (consume("$") || consume("#") || consume("//")) {
            skip_line();
            return res;
        } else if (consume("if")) {
            condition = expression();
            prev_condition = condition;
            if (peek("begin")) {
                res = always_statements();
            } else {
                res = always_statement();
                consume(";");
            }
            for (auto i = res.begin(); i != res.end(); i++) {
                string out = ".temp" + to_string(tempCounter++);
                cout << "wire " << out << " ?: " << condition << " "
                     << i->second << " " << i->first << endl;
                res[i->first] = out;
            }
            // TODO: (treat variables appearing in all blocks and only some
            // blocks differently) do easy option first
            while (consume("else")) {
                if (consume("if")) {
                    // else if block
                    // update conditions to say !prev_condition &&
                    // this.condition

                    string temp = ".temp" + to_string(tempCounter++);
                    cout << "wire " << temp << " ! " << prev_condition
                         << endl;  // temp should have !prev_condition
                    condition = expression();
                    string result_condition =
                        ".temp" + to_string(tempCounter++);
                    cout << "wire " << result_condition << " && " << temp << " "
                         << condition << endl;
                    string new_prev_condition =
                        ".temp" + to_string(tempCounter++);
                    cout << "wire " << new_prev_condition << " && "
                         << prev_condition << " " << condition << endl;
                    prev_condition = new_prev_condition;
                    condition = result_condition;  // condition should be right

                    unordered_map<string, string> inside;
                    if (peek("begin")) {
                        inside = always_statements();
                    } else {
                        inside = always_statement();
                        consume(";");
                    }

                    for (auto i = inside.begin(); i != inside.end(); i++) {
                        string out = ".temp" + to_string(tempCounter++);
                        cout << "wire " << out << " ?: " << condition << " "
                             << i->second << " " << res[i->first] << endl;
                        res[i->first] = out;
                    }
                } else {
                    // else
                    string condition = ".temp" + to_string(tempCounter++);
                    cout << "wire " << condition << " ! " << prev_condition
                         << endl;  // condition should have !prev_condition

                    unordered_map<string, string> inside;
                    if (peek("begin")) {
                        inside = always_statements();
                    } else {
                        inside = always_statement();
                        consume(";");
                    }
                    for (auto i = inside.begin(); i != inside.end(); i++) {
                        string out = ".temp" + to_string(tempCounter++);
                        cout << "wire " << out << " ?: " << condition << " "
                             << i->second << " " << res[i->first] << endl;
                        res[i->first] = out;
                    }
                }
            }
            return res;
        } else if (consume("for")) {
            skip_block();
            return res;
        } else {
            // reg
            if (auto id = consume_identifier()) {
                reg_name = id.value();
                consume("<=");
                string reg_inputs = expression();
                res[reg_name] = reg_inputs;
                return res;
            }
        }
        return res;
    }

    unordered_map<string, string> always_statements() {
        // puff shroom! :( not helping
        unordered_map<string, string> res;
        consume("begin");
        while (!consume("end")) {
            unordered_map<string, string> part = always_statement();
            consume(";");
            if (consume("//")) {
                skip_line();
            }
            for (auto i = part.begin(); i != part.end(); i++) {
                res[i->first] += i->second;
            }
        }
        return res;
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
        for (auto i = reg_table.begin(); i != reg_table.end(); i++) {
            cout << "reg " << i->first << " = " << i->second << endl;
        }
        for (uint32_t i = 0; i < module_names.size(); i++) {
            string output = " # ";
            vector<pair<bool,string>> module = module_list[i];
            for (uint32_t j = 0; j < module.size(); j++) {
                pair<bool, string> param = module[j];
                if (param.first) {
                    output = output + param.second + " ";
                } else {
                    output = " " + param.second + output;
                }
            }
            cout << "module " << module_names[i] << output << endl;
        }
        end_or_fail();
    }
};

int main(int argc, const char* const* const argv) {
    unordered_map<string, pair<bool, string>> modules;
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
