def mySearch(theList, anItem):
    return recursiveSearch(theList, 0, len(theList)-1, anItem)

def recursiveSearch(theList, left, right, anItem):
    if right < left:
        return False
    else:
        mid = (right+left)//2
        if theList[mid] == anItem:
            return True
        elif anItem > theList[mid]:
            ##Added for conflict
            left = mid+1
            return recursiveSearch(theList, left, right, anItem)
        else:
            ##Added for conflict
            right = mid-1
            return recursiveSearch(theList, left, right, anItem)

def main():
    aList = ["apple", "jacks", "peanuts", "quail", "snail", "tilapia", "town", "zebra"]
    print(mySearch(aList, "peanuts"))
    
main()
    