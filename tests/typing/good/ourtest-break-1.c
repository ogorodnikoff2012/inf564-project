int main() {
  int x;
  x = 0;

  while (1) {
    if (x >= 5) {
      break;
    }

    x = x + 1;
  }

  putchar(48 + x);
  putchar(10);

  return 0;
}