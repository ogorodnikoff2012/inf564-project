int main() {
  int x;

  for (x = 0; x < 10; x = x + 1) {
    if (x % 2 == 0) {
      continue;
    }
    putchar(48 + x);
  }
  putchar(10);

  return 0;
}