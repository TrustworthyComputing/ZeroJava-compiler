all: compile

compile:
	cd compiler/src/zilch && $(MAKE)

clean:
	cd compiler/src/zilch && $(MAKE) clean
	rm ./compiler/zilch-examples/*.asm
