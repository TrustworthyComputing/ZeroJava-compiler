all: compile

compile:
	cd compiler/src/zilch && $(MAKE)

clean:
	cd compiler/src/zilch && $(MAKE) clean
	./compiler/zilch-examples/*.asm
