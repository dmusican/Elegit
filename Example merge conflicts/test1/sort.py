def mySearch(theList, anItem):
    for i in range(0, len(theList)-1, -1):
        if theList[i] == anItem:
            print("Found: " + str(anItem))
<<<<<<< HEAD
        else:
            print("Sad")
=======
    print("Sad")
>>>>>>> 3d77f056fb4e26b1ea31ff82abd52443a923b601
    return

def main():
    aList = []
    for i in range(400):
        aList.append(i * 3 // 2 + (50%(i+1)))
    mySearch(aList, 8)
    
main()
    