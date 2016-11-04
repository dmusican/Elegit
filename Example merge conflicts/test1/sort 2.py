def mySearch(theList, anItem):
    for i in range(0, len(theList)-1, -1):
        if theList[i] == anItem:
            print("Found: " + str(anItem))
    print("Sad")
    return

def main():
    aList = []
    for i in range(400):
        aList.append(i * 3 // 2 + (50%(i+1)))
    mySearch(aList, 8)
    
main()
    