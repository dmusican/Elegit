def mySearch(theList, anItem):
    return recursiveSearch(theList, 0, len(theList)-1, anItem)

def recursiveSearch(theList, left, right, anItem):
    if right < left:
        return False
    else:
        mid = (right+left)//2
        if theList[mid] == anItem:
            return True
<<<<<<< HEAD
        elif anItem < theList[mid]:
            return recursiveSearch(theList, left, mid-1, anItem)
        else:
            return recursiveSearch(theList, mid+1, right, anItem)
=======
        elif anItem > theList[mid]:
            left = mid+1
            return recursiveSearch(theList, left, right, anItem)
        else:
            right = mid-1
            return recursiveSearch(theList, left, right, anItem)
>>>>>>> a25288eb304e22c8596ac86c4273c980ca5d68fb

def main():
    aList = ["apple", "jacks", "peanuts", "quail", "snail", "tilapia", "town", "zebra"]
<<<<<<< HEAD
    print(recursiveSearch(aList, 0, 8, "quail"))
=======
    print(mySearch(aList, "peanuts"))
>>>>>>> e658c092fce5f7902fdb62c19388205d16dfead5
    
main()
    