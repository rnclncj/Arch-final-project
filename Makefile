B=build
CXX=g++
CXX_FLAGS=-Wall -Werror -O5 -std=c++17

CXX_FILES=${wildcard *.cxx}
CXX_O_FILES=${addprefix $B/,${subst .cxx,.o,${CXX_FILES}}}

LINK=${firstword ${patsubst %.cxx,${CXX},${CXX_FILES} ${patsubst %.c,${CC},${C_FILES}}}}

V_FILES=${wildcard *.v}
TESTS=${subst .v,.test,${V_FILES}}
VF_FILES=${subst .v,.vf,${V_FILES}}

all : $B/main

test : Makefile ${TESTS}

$B/main: ${CXX_O_FILES}
	@mkdir -p build
	${LINK} -o $@ ${CXX_O_FILES}

${CXX_O_FILES} : $B/%.o: %.cxx Makefile
	@mkdir -p build
	${CXX} -MMD -MF $B/$*.d -c -o $@ ${CXX_FLAGS} $*.cxx
	
${VF_FILES}: %.vf : Makefile $B/main %.v
	@echo "failed to run" > $@
	$B/main $*.v > $@

-include $B/*.d

clean:
	rm -rf build
	rm -f *.vf 