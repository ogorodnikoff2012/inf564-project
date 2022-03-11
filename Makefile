SRC=src/mini_c/RTL.java \
	src/mini_c/ToERTL.java \
	src/mini_c/Register.java \
	src/mini_c/TypeError.java \
	src/mini_c/Memory.java \
	src/mini_c/ERTLinterp.java \
	src/mini_c/ToLTL.java \
	src/mini_c/Label.java \
	src/mini_c/Ttree.java \
	src/mini_c/LTLinterp.java \
	src/mini_c/MyParser.java \
	src/mini_c/LTL.java \
	src/mini_c/ConstEvalOptimizer.java \
	src/mini_c/Ptree.java \
	src/mini_c/ToRTL.java \
	src/mini_c/Machine.java \
	src/mini_c/Typing.java \
	src/mini_c/CompilerError.java \
	src/mini_c/RTLinterp.java \
	src/mini_c/X86_64.java \
	src/mini_c/Main.java \
	src/mini_c/ERTL.java \
	src/mini_c/Liveness.java \
	src/mini_c/SyntaxError.java \
	src/mini_c/Lin.java \
	src/mini_c/LexicalError.java \
	src/mini_c/Ops.java

SRC_GEN=src/mini_c/Parser.java src/mini_c/sym.java src/mini_c/Lexer.java

all: $(SRC_GEN) minic bin
	javac -cp lib/java-cup-11a.jar:bin -d bin $(SRC) $(SRC_GEN)

bin:
	mkdir -p bin

.PHONY: clean

src/mini_c/Parser.java:
	java -jar lib/java-cup-11a.jar -package mini_c -parser Parser -destdir src/mini_c src/mini_c/Parser.cup

src/mini_c/sym.java: src/mini_c/Parser.java

src/mini_c/Lexer.java:
	jflex src/mini_c/Lexer.flex

clean:
	rm -rf bin minic

minic:
	cp mini-c minic

