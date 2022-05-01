B=build
CXX=g++
CXX_FLAGS=-Wall -Werror -g -std=c++17
CC=cc
CC_FLAGS=-Wall -Werror -g -std=c99


CXX_FILES=${wildcard *.cxx}
CXX_O_FILES=${addprefix $B/,${subst .cxx,.o,${CXX_FILES}}}

C_FILES=${wildcard *.c}
C_O_FILES=${addprefix $B/,${subst .c,.o,${C_FILES}}}

LINK=${firstword ${patsubst %.cxx,${CXX},${CXX_FILES} ${patsubst %.c,${CC},${C_FILES}}}}
LINK_FLAGS=

V_FILES=${wildcard *.v}
TESTS=${subst .v,.test,${V_FILES}}
VF_FILES=${subst .v,.vf,${V_FILES}}

all : $B/main

test : Makefile ${TESTS}

$B/main: ${CXX_O_FILES} ${C_O_FILES}
	@mkdir -p build
	${LINK} -o $@ ${LINK_FLAGS} ${CXX_O_FILES} ${C_O_FILES}

${CXX_O_FILES} : $B/%.o: %.cxx Makefile
	@mkdir -p build
	${CXX} -MMD -MF $B/$*.d -c -o $@ ${CXX_FLAGS} $*.cxx

${C_O_FILES} : $B/%.o: %.c Makefile
	@mkdir -p build
	${CC} -MMD -MF $B/$*.d -c -o $@ ${CC_FLAGS} $*.c

${TESTS}: %.test : Makefile %.result
	echo "$* ... $$(cat $*.result) [$$(cat $*.time)]"
	
${VF_FILES}: %.vf : Makefile $B/main %.v
	@echo "failed to run" > $@
	$B/main $*.v > $@

-include $B/*.d

clean:
	rm -rf build
	rm -f *.vf 
