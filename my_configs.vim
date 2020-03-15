filetype on
filetype indent on
syntax on

set nofoldenable
set number
set cursorline
set wildmenu
set lazyredraw
set showmatch
set incsearch
set hlsearch

colorscheme default
"colorscheme gruvbox

let mapleader = ","
imap kj <Esc>
noremap <Leader>w :wa<CR>
noremap <Leader>q :wq<CR>
nnoremap <leader><space> :nohlsearch<CR>
nnoremap B ^
nnoremap E $
nnoremap <C-n> :bnext<CR>
nnoremap <C-p> :bprevious<CR>

" Set tab stops to 2 spaces
"set tabstop=2 softtabstop=0 expandtab shiftwidth=2 smarttab
set tabstop=8 softtabstop=0 expandtab shiftwidth=4 smarttab
